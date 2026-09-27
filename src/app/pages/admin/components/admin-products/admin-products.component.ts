import { Component, EventEmitter, Input, Output } from '@angular/core';

import { DecimalPipe } from '@angular/common';

import { Product } from '../../../../models/product';
import { ProductService } from '../../../../services/product.service';

@Component({
  selector: 'app-admin-products',
  imports: [DecimalPipe],
  templateUrl: './admin-products.component.html',
  styleUrl: './admin-products.component.scss',
})
export class AdminProductsComponent {
  @Input() searchTerm = '';

  @Output() editProduct = new EventEmitter<Product>();

  products: Product[] = [];
  filteredProducts: Product[] = [];

  constructor(private productService: ProductService) {
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
    product.active = !product.active;

    this.productService.updateProduct(product);

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
