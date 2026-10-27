import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable, Subject } from 'rxjs';
import {
  Product,
  CreateProduct,
  ProductDetail,
  ProductOption,
} from '../models/product';

@Injectable({
  providedIn: 'root',
})
export class ProductService {
  private apiUrl = 'http://localhost:8080/products';
  private refreshSubject = new Subject<void>();

  refresh$ = this.refreshSubject.asObservable();

  constructor(private http: HttpClient) {}

  refresh() {
    this.refreshSubject.next();
  }

  // =========================
  // CONSULTAS
  // =========================

  getProducts(): Observable<Product[]> {
    return this.http.get<Product[]>(this.apiUrl);
  }

  getActiveProducts(): Observable<Product[]> {
    return this.http.get<Product[]>(`${this.apiUrl}/active`);
  }

  // Solo id y nombre de todos los productos (filtros del admin)
  getProductOptions(): Observable<ProductOption[]> {
    return this.http.get<ProductOption[]>(`${this.apiUrl}/options`);
  }

  getProductById(id: number): Observable<ProductDetail> {
    return this.http.get<ProductDetail>(`${this.apiUrl}/${id}`);
  }

  getPopularProducts(): Observable<Product[]> {
    return this.http.get<Product[]>(`${this.apiUrl}/popular`);
  }

  getRelatedProducts(id: number): Observable<Product[]> {
    return this.http.get<Product[]>(`${this.apiUrl}/${id}/related`);
  }

  getActiveProductIds(): Observable<number[]> {
    return this.http.get<number[]>(`${this.apiUrl}/active/ids`);
  }

  // =========================
  // CRUD
  // =========================

  createProduct(product: CreateProduct): Observable<Product> {
    return this.http.post<Product>(this.apiUrl, this.toPayload(product));
  }

  updateProduct(product: Product): Observable<Product> {
    return this.http.put<Product>(
      `${this.apiUrl}/${product.id}`,
      this.toPayload(product),
    );
  }

  deleteProduct(id: number): Observable<void> {
    return this.http.delete<void>(`${this.apiUrl}/${id}`);
  }

  toggleProductActiveStatus(id: number): Observable<Product> {
    return this.http.put<Product>(`${this.apiUrl}/${id}/toggle`, {});
  }

  private toPayload(product: Product | CreateProduct) {
    return {
      ...product,
      category: product.category
        ? {
            id: product.category.id,
          }
        : null,
    };
  }
}
