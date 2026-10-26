import { Component, EventEmitter, Input, Output, inject } from '@angular/core';
import { EmptyStateComponent } from '../../../../components/empty-state/empty-state.component';
import { AdminProductRowComponent } from '../admin-product-row/admin-product-row.component';
import { Product } from '../../../../models/product';
import { ProductService } from '../../../../services/product.service';
import {
  ModalComponent,
  getErrorMessage,
} from '../../../../components/modal/modal.component';
import { ToastService } from '../../../../services/toast.service';

@Component({
  selector: 'app-admin-products',
  imports: [AdminProductRowComponent, EmptyStateComponent, ModalComponent],
  templateUrl: './admin-products.component.html',
})
export class AdminProductsComponent {
  private productService = inject(ProductService);
  private toast = inject(ToastService);

  @Input() searchTerm = '';

  @Output() editProduct = new EventEmitter<Product>();

  readonly columns = ['Comida', 'Categoría', 'Precio', 'Estado', 'Acciones'];

  products: Product[] = [];
  filteredProducts: Product[] = [];

  showDeleteModal = false;
  showErrorModal = false;

  selectedProductId: number | null = null;

  confirmTitle = 'Eliminar producto';
  confirmMessage =
    '¿Estás seguro de que deseas eliminar este producto de la carta?';

  errorTitle = '';
  errorMessage = '';

  constructor() {
    this.loadProducts();

    this.productService.refresh$.subscribe(() => {
      this.loadProducts();
    });
  }

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

  ngOnChanges() {
    this.filterProducts();
  }

  filterProducts() {
    const value = this.searchTerm.trim().toLowerCase();

    this.filteredProducts = this.products.filter((product) =>
      product.name.toLowerCase().includes(value),
    );
  }

  toggleProduct(product: Product) {
    this.productService.toggleProductActiveStatus(product.id).subscribe({
      next: () => {
        this.productService.refresh();
      },
      error: (error) => {
        console.error('Error cambiando estado del producto:', error);
      },
    });
  }

  removeProduct(product: Product) {
    this.selectedProductId = product.id;
    this.showDeleteModal = true;
  }

  cancelDelete() {
    this.showDeleteModal = false;
    this.selectedProductId = null;
  }

  confirmDelete() {
    if (this.selectedProductId === null) {
      return;
    }

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

  private openErrorModal(title: string, message: string) {
    this.errorTitle = title;
    this.errorMessage = message;
    this.showErrorModal = true;
  }

  closeErrorModal() {
    this.showErrorModal = false;
    this.errorTitle = '';
    this.errorMessage = '';
  }

  openEdit(product: Product) {
    this.editProduct.emit(product);
  }
}
