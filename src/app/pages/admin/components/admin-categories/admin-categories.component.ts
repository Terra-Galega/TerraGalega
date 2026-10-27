import { Component, EventEmitter, Input, Output, inject } from '@angular/core';
import { EmptyStateComponent } from '../../../../components/empty-state/empty-state.component';
import { IconButtonComponent } from '../../../../components/icon-button/icon-button.component';
import { Category } from '../../../../models/category';
import { CategoryService } from '../../../../services/category.service';
import { switchMap, map } from 'rxjs/operators';
import { AddOnService } from '../../../../services/addOn.service';
import { ModalComponent } from '../../../../components/modal/modal.component';
import { ToastService } from '../../../../services/toast.service';
import { getErrorMessage } from '../../../../utils/http-error';

@Component({
  selector: 'app-admin-categories',
  imports: [EmptyStateComponent, IconButtonComponent, ModalComponent],
  templateUrl: './admin-categories.component.html',
})
export class AdminCategoriesComponent {
  private categoryService = inject(CategoryService);
  private addOnService = inject(AddOnService);
  private toast = inject(ToastService);

  @Input() searchTerm = '';

  @Output() editCategory = new EventEmitter<Category>();

  readonly columns = ['Nombre', 'Descripción', 'Adicionales', 'Acciones'];

  categories: Category[] = [];
  filteredCategories: Category[] = [];

  showDeleteModal = false;
  showErrorModal = false;

  selectedCategoryId: number | null = null;

  confirmTitle = 'Eliminar categoría';
  confirmMessage = '¿Estás seguro de que deseas eliminar esta categoría?';

  errorTitle = '';
  errorMessage = '';
  errorItems: string[] = [];
  errorWarning = '';

  constructor() {
    this.loadCategories();

    this.categoryService.refresh$.subscribe(() => {
      this.loadCategories();
    });
  }

  loadCategories() {
    this.categoryService
      .getCategories()
      .pipe(
        switchMap((categories) =>
          this.addOnService.getAddOns().pipe(
            map((addOns) =>
              categories.map((category) => ({
                ...category,
                addOns: addOns.filter((addOn) =>
                  addOn.categories?.some((c) => c.id === category.id),
                ),
              })),
            ),
          ),
        ),
      )
      .subscribe({
        next: (categories) => {
          this.categories = categories;
          this.filterCategories();
        },
        error: (error) => {
          console.error('Error cargando categorías:', error);
        },
      });
  }

  ngOnChanges() {
    this.filterCategories();
  }

  filterCategories() {
    const value = this.searchTerm.trim().toLowerCase();

    this.filteredCategories = this.categories.filter((category) =>
      category.name.toLowerCase().includes(value),
    );
  }

  openEdit(category: Category) {
    this.editCategory.emit(category);
  }

  removeCategory(category: Category) {
    this.selectedCategoryId = category.id;
    this.showDeleteModal = true;
  }

  cancelDelete() {
    this.showDeleteModal = false;
    this.selectedCategoryId = null;
  }

  confirmDelete() {
    if (this.selectedCategoryId === null) {
      return;
    }

    const id = this.selectedCategoryId;

    this.showDeleteModal = false;
    this.selectedCategoryId = null;

    this.categoryService.deleteCategory(id).subscribe({
      next: () => {
        this.categoryService.refresh();
        this.toast.show('Categoría eliminada correctamente');
      },
      error: (error) => {
        console.error('Error eliminando categoría:', error);

        const products: string[] | undefined = error?.error?.products;

        if (products?.length) {
          this.openErrorModal(
            'No se pudo eliminar la categoría',
            getErrorMessage(error),
            products,
            'No puede tener productos sin categoría, primero asócielos a una diferente.',
          );
          return;
        }

        this.openErrorModal(
          'No se pudo eliminar la categoría',
          getErrorMessage(
            error,
            'Ocurrió un error al eliminar la categoría. Por favor, inténtalo de nuevo.',
          ),
        );
      },
    });
  }

  private openErrorModal(
    title: string,
    message: string,
    items: string[] = [],
    warning = '',
  ) {
    this.errorTitle = title;
    this.errorMessage = message;
    this.errorItems = items;
    this.errorWarning = warning;
    this.showErrorModal = true;
  }

  closeErrorModal() {
    this.showErrorModal = false;
    this.errorTitle = '';
    this.errorMessage = '';
    this.errorItems = [];
    this.errorWarning = '';
  }
}