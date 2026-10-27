import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable, Subject } from 'rxjs';
import {
  Product,
  CreateProduct,
  ProductDetail,
  ProductOption,
} from '../models/product';

/**
 * Servicio que habla con el back en /products: consultas del menú y CRUD del
 * admin. Es un servicio único (providedIn: 'root'), compartido por toda la app.
 */
@Injectable({
  providedIn: 'root',
})
export class ProductService {
  // Dirección base de los endpoints de productos en el back
  private apiUrl = 'http://localhost:8080/products';

  // Aviso de "los productos cambiaron": quien modifica un producto llama a
  // refresh() y los componentes que escuchan refresh$ recargan su lista
  private refreshSubject = new Subject<void>();

  refresh$ = this.refreshSubject.asObservable();

  constructor(private http: HttpClient) {}

  // Dispara el aviso para que las listas se vuelvan a cargar
  refresh() {
    this.refreshSubject.next();
  }

  // =========================
  // CONSULTAS
  // =========================

  // Todos los productos, activos e inactivos (admin)
  getProducts(): Observable<Product[]> {
    return this.http.get<Product[]>(this.apiUrl);
  }

  // Solo los productos activos (menú del cliente)
  getActiveProducts(): Observable<Product[]> {
    return this.http.get<Product[]>(`${this.apiUrl}/active`);
  }

  // Solo id y nombre de todos los productos (filtros del admin)
  getProductOptions(): Observable<ProductOption[]> {
    return this.http.get<ProductOption[]>(`${this.apiUrl}/options`);
  }

  // Un producto con su detalle (la categoría trae descripción)
  getProductById(id: number): Observable<ProductDetail> {
    return this.http.get<ProductDetail>(`${this.apiUrl}/${id}`);
  }

  // Productos populares, para "Favoritos de la casa"
  getPopularProducts(): Observable<Product[]> {
    return this.http.get<Product[]>(`${this.apiUrl}/popular`);
  }

  // Hasta 3 productos de la misma categoría, para "También te puede gustar"
  getRelatedProducts(id: number): Observable<Product[]> {
    return this.http.get<Product[]>(`${this.apiUrl}/${id}/related`);
  }

  // Solo los ids de los productos activos (para validar el carrito)
  getActiveProductIds(): Observable<number[]> {
    return this.http.get<number[]>(`${this.apiUrl}/active/ids`);
  }

  // =========================
  // CRUD
  // =========================

  // Crea un producto nuevo
  createProduct(product: CreateProduct): Observable<Product> {
    return this.http.post<Product>(this.apiUrl, this.toPayload(product));
  }

  // Actualiza un producto existente (el id va en la dirección)
  updateProduct(product: Product): Observable<Product> {
    return this.http.put<Product>(
      `${this.apiUrl}/${product.id}`,
      this.toPayload(product),
    );
  }

  // Elimina un producto (el back lo rechaza si está en pedidos)
  deleteProduct(id: number): Observable<void> {
    return this.http.delete<void>(`${this.apiUrl}/${id}`);
  }

  // Alterna el producto entre activo e inactivo
  toggleProductActiveStatus(id: number): Observable<Product> {
    return this.http.put<Product>(`${this.apiUrl}/${id}/toggle`, {});
  }

  // Prepara el producto para enviarlo: de la categoría solo se manda el id,
  // porque el back busca la categoría real por ese id
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
