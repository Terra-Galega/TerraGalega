import {
  Component,
  EventEmitter,
  Input,
  OnChanges,
  Output,
  inject,
} from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { EmptyStateComponent } from '../../../../components/empty-state/empty-state.component';
import { SegmentedControlComponent } from '../../../../components/segmented-control/segmented-control.component';
import { AdminProductRowComponent } from '../admin-product-row/admin-product-row.component';
import { CategoryFilterComponent } from '../../../menu/components/category-filter/category-filter.component';
import { FeatureFilterComponent } from '../../../menu/components/feature-filter/feature-filter.component';
import { Product } from '../../../../models/product';
import { Category } from '../../../../models/category';
import { FeatureKey } from '../../../../models/feature';
import { ProductService } from '../../../../services/product.service';
import { CategoryService } from '../../../../services/category.service';
import { ModalComponent } from '../../../../components/modal/modal.component';
import { ToastService } from '../../../../services/toast.service';
import { getErrorMessage } from '../../../../utils/http-error';

// Filtro por estado del plato (solo existe en el admin)
type StatusFilter = 'all' | 'active' | 'inactive';

/**
 * Tabla de productos del panel de administración.
 * Carga los productos y los filtra por texto de búsqueda, categoría (incluida
 * "Popular"), estado y características. Los cuatro filtros se combinan.
 * Permite activar/desactivar, editar y eliminar (con ventana de confirmación
 * y ventana de error si el servidor rechaza la eliminación).
 */
@Component({
  selector: 'app-admin-products',
  imports: [
    AdminProductRowComponent,
    EmptyStateComponent,
    ModalComponent,
    CategoryFilterComponent,
    FeatureFilterComponent,
    SegmentedControlComponent,
  ],
  templateUrl: './admin-products.component.html',
})
export class AdminProductsComponent implements OnChanges {
  private productService = inject(ProductService);
  private categoryService = inject(CategoryService);
  private toast = inject(ToastService);

  // Texto de búsqueda que escribe el admin en la barra superior
  @Input() searchTerm = '';

  // Avisa al padre qué producto se quiere editar (el padre abre el modal)
  @Output() editProduct = new EventEmitter<Product>();

  // Encabezados de la tabla
  readonly columns = ['Comida', 'Categoría', 'Precio', 'Estado', 'Acciones'];

  // Todos los productos que devuelve el servidor
  products: Product[] = [];
  // Productos que cumplen los filtros (los que se dibujan)
  filteredProducts: Product[] = [];
  // Categorías del servidor, para armar los botones de filtro
  categories: Category[] = [];

  // Estado actual de los filtros
  currentCategory = 'Todos';
  statusFilter: StatusFilter = 'all';
  activeFeatureFilters: FeatureKey[] = [];

  // Opciones del selector de estado
  readonly statusOptions: { value: StatusFilter; label: string }[] = [
    { value: 'all', label: 'Todos' },
    { value: 'active', label: 'Activos' },
    { value: 'inactive', label: 'Inactivos' },
  ];

  // Controlan la ventana de confirmación y la ventana de error
  showDeleteModal = false;
  showErrorModal = false;

  // Id del producto que se está a punto de eliminar
  selectedProductId: number | null = null;

  // Textos de la ventana de confirmación
  confirmTitle = 'Eliminar producto';
  confirmMessage =
    '¿Estás seguro de que deseas eliminar este producto de la carta?';

  // Textos de la ventana de error (se llenan según el fallo)
  errorTitle = '';
  errorMessage = '';

  constructor() {
    this.loadProducts();

    // Las categorías se piden una sola vez para armar los botones de filtro
    this.categoryService.getCategories().subscribe({
      next: (categories) => {
        this.categories = categories;
      },
      error: (error) => {
        console.error('Error cargando categorías:', error);
      },
    });

    // Cada vez que otro componente pide refrescar (por ejemplo, tras guardar
    // en el modal), se vuelve a cargar la lista. takeUntilDestroyed evita que
    // la suscripción quede viva al cambiar de pestaña.
    this.productService.refresh$
      .pipe(takeUntilDestroyed())
      .subscribe(() => {
        this.loadProducts();
      });
  }

  // Nombres de los botones de categoría: "Todos" y "Popular" son fijos y
  // después van las categorías reales
  get categoriesList(): string[] {
    return [
      'Todos',
      'Popular',
      ...this.categories.map((category) => category.name),
    ];
  }

  // true si hay algún filtro del panel aplicado (sin contar la búsqueda)
  get hasFilters(): boolean {
    return (
      this.currentCategory !== 'Todos' ||
      this.statusFilter !== 'all' ||
      this.activeFeatureFilters.length > 0
    );
  }

  // Pide los productos al servidor y vuelve a aplicar los filtros
  loadProducts() {
    this.productService.getProducts().subscribe({
      next: (products) => {
        this.products = products;
        this.filterProducts();
      },
      error: (error) => {
        console.error('Error cargando productos:', error);
      },
    });
  }

  // Angular lo llama cuando cambia un @Input (aquí, searchTerm)
  ngOnChanges() {
    this.filterProducts();
  }

  // Aplica los cuatro filtros. Un producto se muestra solo si cumple
  // búsqueda, categoría, estado y características a la vez.
  filterProducts() {
    const search = this.searchTerm.trim().toLowerCase();

    this.filteredProducts = this.products.filter((product) => {
      const matchesSearch = product.name.toLowerCase().includes(search);

      // "Todos" no filtra; "Popular" usa la marca popular del producto;
      // cualquier otra opción compara con el nombre de su categoría
      const matchesCategory =
        this.currentCategory === 'Todos' ||
        (this.currentCategory === 'Popular'
          ? product.popular
          : product.category?.name === this.currentCategory);

      const matchesStatus =
        this.statusFilter === 'all' ||
        (this.statusFilter === 'active' ? product.active : !product.active);

      // Con varias características, el plato debe cumplirlas todas
      const matchesFeatures = this.activeFeatureFilters.every(
        (feature) => product[feature],
      );

      return matchesSearch && matchesCategory && matchesStatus && matchesFeatures;
    });
  }

  // El admin eligió otro botón de categoría
  selectCategory(category: string) {
    this.currentCategory = category;
    this.filterProducts();
  }

  // El admin cambió el selector Todos / Activos / Inactivos
  selectStatus(value: string) {
    this.statusFilter = value as StatusFilter;
    this.filterProducts();
  }

  // El admin aplicó (o limpió) el panel de características
  applyFeatureFilters(features: FeatureKey[]) {
    this.activeFeatureFilters = features;
    this.filterProducts();
  }

  // Quita categoría, estado y características (la búsqueda de la barra
  // superior la maneja el padre y no se toca)
  resetFilters() {
    this.currentCategory = 'Todos';
    this.statusFilter = 'all';
    this.activeFeatureFilters = [];
    this.filterProducts();
  }

  // Activa o desactiva el producto (botón "Activo/Inactivo" de la fila)
  toggleProduct(product: Product) {
    this.productService.toggleProductActiveStatus(product.id).subscribe({
      next: () => {
        // Se recarga la lista para ver el nuevo estado
        this.productService.refresh();
      },
      error: (error) => {
        console.error('Error cambiando estado del producto:', error);
      },
    });
  }

  // Paso 1 de eliminar: guarda cuál es el producto y abre la confirmación
  removeProduct(product: Product) {
    this.selectedProductId = product.id;
    this.showDeleteModal = true;
  }

  // El admin se arrepintió: se cierra la confirmación y se olvida el producto
  cancelDelete() {
    this.showDeleteModal = false;
    this.selectedProductId = null;
  }

  // Paso 2 de eliminar: el admin confirmó
  confirmDelete() {
    if (this.selectedProductId === null) {
      return;
    }

    // Se guarda el id en una variable local porque se limpia justo después
    const id = this.selectedProductId;

    this.showDeleteModal = false;
    this.selectedProductId = null;

    this.productService.deleteProduct(id).subscribe({
      next: () => {
        this.productService.refresh();
        this.toast.show('Producto eliminado correctamente');
      },
      error: (error) => {
        console.error('Error eliminando producto:', error);
        // Se muestra el mensaje que mandó el servidor, o uno genérico
        this.openErrorModal(
          'No se pudo eliminar el producto',
          getErrorMessage(
            error,
            'Ocurrió un error al eliminar el producto. Por favor, inténtalo de nuevo.',
          ),
        );
      },
    });
  }

  // Abre la ventana de error con el título y mensaje indicados
  private openErrorModal(title: string, message: string) {
    this.errorTitle = title;
    this.errorMessage = message;
    this.showErrorModal = true;
  }

  // Cierra la ventana de error y limpia sus textos
  closeErrorModal() {
    this.showErrorModal = false;
    this.errorTitle = '';
    this.errorMessage = '';
  }

  // Reenvía al padre el producto que se quiere editar
  openEdit(product: Product) {
    this.editProduct.emit(product);
  }
}