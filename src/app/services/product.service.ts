import { Injectable, inject } from '@angular/core';
import { Product } from '../models/product';
import { CategoryService } from './category.service';
import { HttpClient } from '@angular/common/http';

@Injectable({
  providedIn: 'root',
})
export class ProductService {
  constructor(private http: HttpClient) {}

  getProducts(): Observable<Product[]> {
    return this.http.get<Product[]>('http://localhost:8080/admin/');
  }

  getActiveProducts() {
    return this.productArray.filter((product) => product.active);
  }

  getProductById(id: number) {
    return this.productArray.find((product) => product.id === id);
  }

  getPopularProducts() {
    return this.productArray.filter((product) => product.popular);
  }

  private categoryService = inject(CategoryService);

  createProduct(product: Product): void {
    this.productArray = [...this.productArray, product];
  }

  updateProduct(product: Product): void {
    this.productArray = this.productArray.map((p) =>
      p.id === product.id ? product : p,
    );
  }

  deleteProduct(id: number): void {
    this.productArray = this.productArray.filter(
      (product) => product.id !== id,
    );
  }

  toggleProductActiveStatus(id: number): void {
    const product = this.getProductById(id);
    if (product) {
      product.active = !product.active;
    }
  }
}
