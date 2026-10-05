import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable, Subject } from 'rxjs';
import { Product } from '../models/product';

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

  getProductById(id: number): Observable<Product> {
    return this.http.get<Product>(`${this.apiUrl}/${id}`);
  }

  getPopularProducts(): Observable<Product[]> {
    return this.http.get<Product[]>(`${this.apiUrl}/popular`);
  }

  // =========================
  // CRUD
  // =========================

  createProduct(product: Product): Observable<Product> {
    return this.http.post<Product>(this.apiUrl, product);
  }
  updateProduct(product: Product): Observable<Product> {
    return this.http.put<Product>(`${this.apiUrl}/${product.id}`, product);
  }

  deleteProduct(id: number): Observable<void> {
    return this.http.delete<void>(`${this.apiUrl}/${id}`);
  }
}
