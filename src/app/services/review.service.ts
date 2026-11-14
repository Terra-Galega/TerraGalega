import { API_URL } from '../api-url';
import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable, Subject } from 'rxjs';
import { Review, ReviewRequest } from '../models/review';

/**
 * Servicio que habla con el back en /reviews (reseñas de los clientes).
 * Es un servicio único (providedIn: 'root'), compartido por toda la app.
 */
@Injectable({
  providedIn: 'root',
})
export class ReviewService {
  // Dirección base de los endpoints de reseñas en el back
  private apiUrl = `${API_URL}/reviews`;

  // Aviso de "las reseñas cambiaron": quien guarda, oculta o borra una llama a
  // refresh() y los componentes que escuchan refresh$ recargan sus datos
  private refreshSubject = new Subject<void>();

  refresh$ = this.refreshSubject.asObservable();

  constructor(private http: HttpClient) {}

  // Dispara el aviso para que las listas se vuelvan a cargar
  refresh() {
    this.refreshSubject.next();
  }

  // =========================
  // Público y cliente
  // =========================

  // Reseñas visibles, de la más reciente a la más antigua (público)
  getReviews(): Observable<Review[]> {
    return this.http.get<Review[]>(this.apiUrl);
  }

  // Mi reseña. El back responde 204 (sin cuerpo) si todavía no he escrito una,
  // y en ese caso llega null.
  getMyReview(): Observable<Review | null> {
    return this.http.get<Review | null>(`${this.apiUrl}/me`);
  }

  // Crea mi reseña o actualiza la que ya tengo (solo se permite una por cliente)
  saveMyReview(review: ReviewRequest): Observable<Review> {
    return this.http.put<Review>(`${this.apiUrl}/me`, review);
  }

  // Elimina mi reseña
  deleteMyReview(): Observable<void> {
    return this.http.delete<void>(`${this.apiUrl}/me`);
  }

  // =========================
  // Admin
  // =========================

  // Todas las reseñas, visibles y ocultas
  getAllReviews(): Observable<Review[]> {
    return this.http.get<Review[]>(`${this.apiUrl}/all`);
  }

  // Oculta una reseña visible o muestra una oculta
  toggleReview(id: number): Observable<Review> {
    return this.http.put<Review>(`${this.apiUrl}/${id}/toggle`, {});
  }

  // Elimina cualquier reseña
  deleteReview(id: number): Observable<void> {
    return this.http.delete<void>(`${this.apiUrl}/${id}`);
  }
}