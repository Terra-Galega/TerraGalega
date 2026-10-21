import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable, Subject } from 'rxjs';
import { Operator, CreateOperator } from '../models/operator';
import { Role } from '../models/role';

@Injectable({
  providedIn: 'root',
})
export class OperatorService {
  private apiUrl = 'http://localhost:8080/operators';

  // =========================
  // Actualización de operadores
  // =========================

  private refreshSubject = new Subject<void>();

  refresh$ = this.refreshSubject.asObservable();

  refresh() {
    this.refreshSubject.next();
  }

  constructor(private http: HttpClient) {}

  // =========================
  // CRUD conectado a Spring Boot
  // =========================

  getOperators(): Observable<Operator[]> {
    return this.http.get<Operator[]>(this.apiUrl);
  }

  getOperatorById(id: number): Observable<Operator> {
    return this.http.get<Operator>(`${this.apiUrl}/${id}`);
  }

  addOperator(operator: CreateOperator): Observable<Operator> {
    return this.http.post<Operator>(this.apiUrl, operator);
  }

  updateOperator(operator: Operator): Observable<Operator> {
    return this.http.put<Operator>(`${this.apiUrl}/${operator.id}`, operator);
  }

  deleteOperator(id: number): Observable<void> {
    return this.http.delete<void>(`${this.apiUrl}/${id}`);
  }

  toggleOperatorActiveStatus(id: number): Observable<Operator> {
    return this.http.put<Operator>(`${this.apiUrl}/${id}/toggle`, {});
  }
}
