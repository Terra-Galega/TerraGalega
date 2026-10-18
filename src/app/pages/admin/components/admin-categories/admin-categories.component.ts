import { Component, EventEmitter, Input, Output, inject } from '@angular/core';
import { EmptyStateComponent } from '../../../../components/empty-state/empty-state.component';
import { IconButtonComponent } from '../../../../components/icon-button/icon-button.component';
import { Category } from '../../../../models/category';
import { CategoryService } from '../../../../services/category.service';
import { switchMap, map } from 'rxjs/operators';
import { AddOnService } from '../../../../services/addOn.service';

@Component({
  selector: 'app-admin-categories',
  imports: [EmptyStateComponent, IconButtonComponent],
  templateUrl: './admin-categories.component.html',
})
export class AdminCategoriesComponent {
  private categoryService = inject(CategoryService);
  private addOnService = inject(AddOnService);

  @Input() searchTerm = '';

  @Output() editCategory = new EventEmitter<Category>();

  readonly columns = ['Nombre', 'Descripción', 'Adicionales', 'Acciones'];

  categories: Category[] = [];
  filteredCategories: Category[] = [];

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
    const confirmed = window.confirm('¿Eliminar esta categoría?');

    if (!confirmed) {
      return;
    }

    this.categoryService.deleteCategory(category.id).subscribe({
      next: () => {
        this.categoryService.refresh();
      },
      error: (error) => {
        console.error('Error eliminando categoría:', error);
        window.alert(
          error.error?.message ?? 'No se pudo eliminar la categoría.',
        );
      },
    });
  }
}
