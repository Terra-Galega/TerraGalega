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

/**
 * Tabla de categorías del panel de administración.
 * Muestra cada categoría con cuántos adicionales tiene asociados, permite
 * filtrar por nombre, editar y eliminar. Si el servidor no deja eliminar
 * (porque la categoría tiene productos), muestra la lista de esos productos
 * y una advertencia en la ventana de error.
 */
@Component({
  selector: 'app-admin-categories',
  imports: [EmptyStateComponent, IconButtonComponent, ModalComponent],
  templateUrl: './admin-categories.component.html',
})
export class AdminCategoriesComponent {
  private categoryService = inject(CategoryService);
  private addOnService = inject(AddOnService);
  private toast = inject(ToastService);

  // Texto de búsqueda que escribe el admin en la barra superior
  @Input() searchTerm = '';

  // Avisa al padre qué categoría se quiere editar (el padre abre el modal)
  @Output() editCategory = new EventEmitter<Category>();

  // Encabezados de la tabla
  readonly columns = ['Nombre', 'Descripción', 'Adicionales', 'Acciones'];

  // Todas las categorías (con sus adicionales) y las que coinciden con la búsqueda
  categories: Category[] = [];
  filteredCategories: Category[] = [];

  // Controlan la ventana de confirmación y la ventana de error
  showDeleteModal = false;
  showErrorModal = false;

  // Id de la categoría que se está a punto de eliminar
  selectedCategoryId: number | null = null;

  // Textos de la ventana de confirmación
  confirmTitle = 'Eliminar categoría';
  confirmMessage = '¿Estás seguro de que deseas eliminar esta categoría?';

  // Contenido de la ventana de error:
  // título, mensaje, lista de nombres (productos) y advertencia final
  errorTitle = '';
  errorMessage = '';
  errorItems: string[] = [];
  errorWarning = '';

  constructor() {
    this.loadCategories();

    // Cada vez que otro componente pide refrescar (por ejemplo, tras guardar
    // en el modal), se vuelve a cargar la lista
    this.categoryService.refresh$.subscribe(() => {
      this.loadCategories();
    });
  }

  // Carga las categorías y los adicionales, y a cada categoría le asigna
  // los adicionales que la tienen asociada (para mostrar su cantidad)
  loadCategories() {
    this.categoryService
      .getCategories()
      .pipe(
        // Cuando llegan las categorías, se piden los adicionales
        switchMap((categories) =>
          this.addOnService.getAddOns().pipe(
            // Se junta todo: cada categoría recibe sus adicionales
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

  // Angular lo llama cuando cambia un @Input (aquí, searchTerm)
  ngOnChanges() {
    this.filterCategories();
  }

  // Deja solo las categorías cuyo nombre contiene el texto buscado
  // (sin distinguir mayúsculas de minúsculas)
  filterCategories() {
    const value = this.searchTerm.trim().toLowerCase();

    this.filteredCategories = this.categories.filter((category) =>
      category.name.toLowerCase().includes(value),
    );
  }

  // Reenvía al padre la categoría que se quiere editar
  openEdit(category: Category) {
    this.editCategory.emit(category);
  }

  // Paso 1 de eliminar: guarda cuál es la categoría y abre la confirmación
  removeCategory(category: Category) {
    this.selectedCategoryId = category.id;
    this.showDeleteModal = true;
  }

  // El admin se arrepintió: se cierra la confirmación y se olvida la categoría
  cancelDelete() {
    this.showDeleteModal = false;
    this.selectedCategoryId = null;
  }

  // Paso 2 de eliminar: el admin confirmó
  confirmDelete() {
    if (this.selectedCategoryId === null) {
      return;
    }

    // Se guarda el id en una variable local porque se limpia justo después
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

        // Si la categoría tiene productos, el servidor devuelve sus nombres
        // en "products". Se muestran como lista con una advertencia.
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

        // Cualquier otro error: mensaje del servidor o uno genérico
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

  // Abre la ventana de error. "items" y "warning" son opcionales:
  // solo se usan cuando hay productos que impiden eliminar
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

  // Cierra la ventana de error y limpia todos sus textos
  closeErrorModal() {
    this.showErrorModal = false;
    this.errorTitle = '';
    this.errorMessage = '';
    this.errorItems = [];
    this.errorWarning = '';
  }
}
