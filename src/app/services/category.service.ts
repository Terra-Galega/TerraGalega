import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable, Subject } from 'rxjs';
import { Category, CreateCategory } from '../models/category';

/**
 * Servicio que habla con el back en /categories (CRUD de categorías).
 * Es un servicio único (providedIn: 'root'), compartido por toda la app.
 */
@Injectable({
  providedIn: 'root',
})
export class CategoryService {
  // Dirección base de los endpoints de categorías en el back
  private apiUrl = 'http://localhost:8080/categories';

  // Aviso de "las categorías cambiaron": quien modifica una categoría llama a
  // refresh() y los componentes que escuchan refresh$ recargan su lista
  private refreshSubject = new Subject<void>();

  refresh$ = this.refreshSubject.asObservable();

  constructor(private http: HttpClient) {}

  // Dispara el aviso para que las listas se vuelvan a cargar
  refresh() {
    this.refreshSubject.next();
  }

  // Todas las categorías
  getCategories(): Observable<Category[]> {
    return this.http.get<Category[]>(this.apiUrl);
  }

  // Una categoría por su id
  getCategoryById(id: number): Observable<Category> {
    return this.http.get<Category>(`${this.apiUrl}/${id}`);
  }

  // Crea una categoría nueva
  createCategory(category: CreateCategory): Observable<Category> {
    return this.http.post<Category>(this.apiUrl, category);
  }

  // Actualiza una categoría existente
  updateCategory(category: Category): Observable<Category> {
    // Solo se envían los datos propios; los adicionales se manejan desde el adicional
    return this.http.put<Category>(`${this.apiUrl}/${category.id}`, {
      id: category.id,
      name: category.name,
      description: category.description,
    });
  }

  // Elimina una categoría (el back la rechaza si tiene productos)
  deleteCategory(id: number): Observable<void> {
    return this.http.delete<void>(`${this.apiUrl}/${id}`);
  }
}
