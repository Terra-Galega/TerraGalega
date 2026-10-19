import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable, Subject } from 'rxjs';
import { Order, OrderSummary } from '../models/order';

@Injectable({ providedIn: 'root' })
export class OrderService {
  private apiUrl = 'http://localhost:8080/orders';
  private refreshSubject = new Subject<void>();
  refresh$ = this.refreshSubject.asObservable();

  constructor(private http: HttpClient) {}

  refresh() {
    this.refreshSubject.next();
  }

  getOrders(): Observable<Order[]> {
    return this.http.get<Order[]>(this.apiUrl);
  }

  getOrdersByClient(clientId: number): Observable<Order[]> {
    return this.http.get<Order[]>(`${this.apiUrl}/client/${clientId}`);
  }

  getOrderById(orderId: number): Observable<Order> {
    return this.http.get<Order>(`${this.apiUrl}/${orderId}`);
  }

  getActiveOrders(): Observable<OrderSummary[]> {
    return this.http.get<OrderSummary[]>(`${this.apiUrl}/active`);
  }

  getAllOrderSummaries(): Observable<OrderSummary[]> {
    return this.http.get<OrderSummary[]>(`${this.apiUrl}/all`);
  }

  confirmOrder(orderId: number): Observable<Order> {
    return this.http.put<Order>(`${this.apiUrl}/${orderId}/confirm`, {});
  }

  cookOrder(orderId: number, operatorId: number): Observable<Order> {
    return this.http.put<Order>(
      `${this.apiUrl}/${orderId}/cook/${operatorId}`,
      {},
    );
  }

  sendOrder(orderId: number, deliveryPersonId: number): Observable<Order> {
    return this.http.put<Order>(
      `${this.apiUrl}/${orderId}/send/${deliveryPersonId}`,
      {},
    );
  }

  deliverOrder(orderId: number): Observable<Order> {
    return this.http.put<Order>(`${this.apiUrl}/${orderId}/deliver`, {});
  }
}
