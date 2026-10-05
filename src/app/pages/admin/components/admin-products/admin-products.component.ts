import { Component, EventEmitter, Input, Output, inject } from '@angular/core';

import { EmptyStateComponent } from '../../../../components/empty-state/empty-state.component';
import { AdminProductRowComponent } from '../admin-product-row/admin-product-row.component';
import { Product } from '../../../../models/product';
import { ProductService } from '../../../../services/product.service';

@Component({
  selector: 'app-admin-products',
  imports: [AdminProductRowComponent, EmptyStateComponent],
  templateUrl: './admin-products.component.html',
  styleUrl: './admin-products.component.scss',
})
export class AdminProductsComponent {
  private productService = inject(ProductService);

  @Input() searchTerm = '';

  @Output() editProduct = new EventEmitter<Product>();

  readonly columns = ['Comida', 'Categoría', 'Precio', 'Estado', 'Acciones'];

  products: Product[] = [];
  filteredProducts: Product[] = [];

  constructor() {
    this.loadProducts();
  }

  loadProducts() {
    this.products = this.productService.getProducts();
    this.filterProducts();
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
    this.productService.toggleProductActiveStatus(product.id);
    this.loadProducts();
  }

  removeProduct(product: Product) {
    const confirmed = window.confirm('¿Eliminar este producto de la carta?');

    if (!confirmed) {
      return;
    }

    this.productService.deleteProduct(product.id);

    this.loadProducts();
  }

  openEdit(product: Product) {
    this.editProduct.emit(product);
  }
}