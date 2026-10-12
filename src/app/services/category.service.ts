import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable, Subject } from 'rxjs';
import { Category, CreateCategory } from '../models/category';

@Injectable({
  providedIn: 'root',
})
export class CategoryService {
  private apiUrl = 'http://localhost:8080/categories';
  private refreshSubject = new Subject<void>();

  refresh$ = this.refreshSubject.asObservable();

  constructor(private http: HttpClient) {}

  refresh() {
    this.refreshSubject.next();
  }

  getCategories(): Observable<Category[]> {
    return this.http.get<Category[]>(this.apiUrl);
  }

  getCategoryById(id: number): Observable<Category> {
    return this.http.get<Category>(`${this.apiUrl}/${id}`);
  }

  createCategory(category: CreateCategory): Observable<Category> {
    return this.http.post<Category>(this.apiUrl, category);
  }

  updateCategory(category: Category): Observable<Category> {
    // Solo se envían los datos propios; los adicionales se manejan desde el adicional
    return this.http.put<Category>(`${this.apiUrl}/${category.id}`, {
      id: category.id,
      name: category.name,
      description: category.description,
    });
  }

  deleteCategory(id: number): Observable<void> {
    return this.http.delete<void>(`${this.apiUrl}/${id}`);
  }
}
