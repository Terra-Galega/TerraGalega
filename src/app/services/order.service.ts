import { Injectable } from '@angular/core';
import { Observable, Subject } from 'rxjs';
import { Order, OrderSummary } from '../models/order';
import { HttpClient, HttpParams } from '@angular/common/http';

export interface OrderFilters {
  from?: string;
  to?: string;
  productId?: number | null;
  orderId?: number | null;
}
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

  getAllOrderSummaries(filters: OrderFilters = {}): Observable<OrderSummary[]> {
    let params = new HttpParams();

    if (filters.from) params = params.set('from', filters.from);
    if (filters.to) params = params.set('to', filters.to);
    if (filters.productId != null)
      params = params.set('productId', filters.productId);
    if (filters.orderId != null)
      params = params.set('orderId', filters.orderId);

    return this.http.get<OrderSummary[]>(`${this.apiUrl}/all`, { params });
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

  cancelOrder(orderId: number, clientId: number): Observable<Order> {
    return this.http.put<Order>(
      `${this.apiUrl}/${orderId}/cancel`,
      {},
      { params: { clientId } },
    );
  }
}
