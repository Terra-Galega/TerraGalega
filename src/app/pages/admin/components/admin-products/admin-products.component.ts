import { Component, EventEmitter, Input, Output, inject } from '@angular/core';
import { EmptyStateComponent } from '../../../../components/empty-state/empty-state.component';
import { AdminProductRowComponent } from '../admin-product-row/admin-product-row.component';
import { Product } from '../../../../models/product';
import { ProductService } from '../../../../services/product.service';
import { ModalComponent } from '../../../../components/modal/modal.component';
import { ToastService } from '../../../../services/toast.service';
import { getErrorMessage } from '../../../../utils/http-error';

/**
 * Tabla de productos del panel de administración.
 * Carga los productos, los filtra por el texto de búsqueda que llega del padre
 * y permite activar/desactivar, editar y eliminar (con ventana de confirmación
 * y ventana de error si el servidor rechaza la eliminación).
 */
@Component({
  selector: 'app-admin-products',
  imports: [AdminProductRowComponent, EmptyStateComponent, ModalComponent],
  templateUrl: './admin-products.component.html',
})
export class AdminProductsComponent {
  private productService = inject(ProductService);
  private toast = inject(ToastService);

  // Texto de búsqueda que escribe el admin en la barra superior
  @Input() searchTerm = '';

  // Avisa al padre qué producto se quiere editar (el padre abre el modal)
  @Output() editProduct = new EventEmitter<Product>();

  // Encabezados de la tabla
  readonly columns = ['Comida', 'Categoría', 'Precio', 'Estado', 'Acciones'];

  // Todos los productos que devuelve el servidor
  products: Product[] = [];
  // Productos que coinciden con la búsqueda (los que se dibujan)
  filteredProducts: Product[] = [];

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

    // Cada vez que otro componente pide refrescar (por ejemplo, tras guardar
    // en el modal), se vuelve a cargar la lista
    this.productService.refresh$.subscribe(() => {
      this.loadProducts();
    });
  }

  // Pide los productos al servidor y vuelve a aplicar el filtro
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

  // Deja solo los productos cuyo nombre contiene el texto buscado
  // (sin distinguir mayúsculas de minúsculas)
  filterProducts() {
    const value = this.searchTerm.trim().toLowerCase();

    this.filteredProducts = this.products.filter((product) =>
      product.name.toLowerCase().includes(value),
    );
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
