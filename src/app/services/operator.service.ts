import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable, Subject } from 'rxjs';
import { Operator, CreateOperator } from '../models/operator';

// Servicio que centraliza la comunicación con el backend para gestionar operadores.
@Injectable({
  // Se registra en la raíz: existe una única instancia compartida por toda la aplicación.
  providedIn: 'root',
})
export class OperatorService {
  // URL base del recurso de operadores en la API de Spring Boot.
  private apiUrl = 'http://localhost:8080/operators';

  // =========================
  // Actualización de operadores
  // =========================

  // Emisor interno de eventos; no lleva datos (void), solo avisa de que hay que recargar.
  private refreshSubject = new Subject<void>();

  // Versión de solo lectura del emisor para que otros componentes se suscriban sin poder emitir.
  refresh$ = this.refreshSubject.asObservable();

  // Notifica a todos los suscriptores que la lista de operadores debe volver a cargarse.
  refresh() {
    this.refreshSubject.next();
  }

  // Inyecta HttpClient para poder realizar las peticiones al backend.
  constructor(private http: HttpClient) {}

  // =========================
  // CRUD conectado a Spring Boot
  // =========================

  // GET: obtiene la lista completa de operadores.
  getOperators(): Observable<Operator[]> {
    return this.http.get<Operator[]>(this.apiUrl);
  }

  // GET: obtiene un operador concreto a partir de su identificador.
  getOperatorById(id: number): Observable<Operator> {
    return this.http.get<Operator>(`${this.apiUrl}/${id}`);
  }

  // POST: crea un nuevo operador y devuelve el operador creado (con el id asignado por el servidor).
  addOperator(operator: CreateOperator): Observable<Operator> {
    return this.http.post<Operator>(this.apiUrl, operator);
  }

  // PUT: actualiza los datos de un operador existente, identificado por su propio id.
  updateOperator(operator: Operator): Observable<Operator> {
    return this.http.put<Operator>(`${this.apiUrl}/${operator.id}`, operator);
  }

  // DELETE: elimina un operador por su identificador; el servidor no devuelve contenido.
  deleteOperator(id: number): Observable<void> {
    return this.http.delete<void>(`${this.apiUrl}/${id}`);
  }

  // PUT: activa o desactiva la cuenta del operador (el cuerpo va vacío porque el servidor alterna el estado).
  toggleOperatorActiveStatus(id: number): Observable<Operator> {
    return this.http.put<Operator>(`${this.apiUrl}/${id}/toggle`, {});
  }

  // PUT: cambia la contraseña del operador enviando la actual (para verificarla) y la nueva.
  changePassword(
    id: number,
    currentPassword: string,
    newPassword: string,
  ): Observable<void> {
    return this.http.put<void>(`${this.apiUrl}/${id}/password`, {
      currentPassword,
      newPassword,
    });
  }
}
