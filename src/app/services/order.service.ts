import { API_URL } from '../api-url';
import { Injectable } from '@angular/core';
import { Observable, Subject } from 'rxjs';
import { Order, OrderSummary } from '../models/order';
import { HttpClient, HttpParams } from '@angular/common/http';

// Filtros opcionales para consultar el historial de pedidos (panel de admin).
export interface OrderFilters {
  // Rango de fechas del pedido
  from?: string;
  to?: string;
  // Solo pedidos que incluyan este producto
  productId?: number | null;
  // Busca un pedido específico por su número
  orderId?: number | null;
}

// Servicio encargado de gestionar los pedidos y comunicarse con el backend.
// Cubre el ciclo completo: PENDING -> CONFIRMED -> PREPARING -> ON_THE_WAY -> DELIVERED, o CANCELLED si el cliente lo cancela.
@Injectable({ providedIn: 'root' })
export class OrderService {
  // URL base de los endpoints de pedidos en Spring Boot.
  private apiUrl = `${API_URL}/orders`;

  // Notifica a los componentes cuando necesitan volver a cargar los pedidos.
  private refreshSubject = new Subject<void>();

  // Observable al que se suscriben las pantallas de pedidos para recargarse.
  refresh$ = this.refreshSubject.asObservable();

  // Inyecta el cliente HTTP para realizar solicitudes al backend.
  constructor(private http: HttpClient) {}

  // Emite una notificación para solicitar la actualización de los datos.
  refresh() {
    this.refreshSubject.next();
  }

  // =========================
  // Consultas
  // =========================

  // Pedidos del cliente con sesión iniciada (pantalla "Mis pedidos").
  getMyOrders(): Observable<Order[]> {
    return this.http.get<Order[]>(`${this.apiUrl}/me`);
  }

  // Detalle completo de un pedido: productos, adicionales y repartidor.
  getOrderById(orderId: number): Observable<Order> {
    return this.http.get<Order>(`${this.apiUrl}/${orderId}`);
  }

  // Resumen de los pedidos en curso (los que el operador debe atender).
  getActiveOrders(): Observable<OrderSummary[]> {
    return this.http.get<OrderSummary[]>(`${this.apiUrl}/active`);
  }

    // Todos los pedidos con sus platos y adicionales (lo usa el dashboard del admin).
  getAllOrders(): Observable<Order[]> {
    return this.http.get<Order[]>(this.apiUrl);
  }

  // Resumen de todos los pedidos, con filtros opcionales.
  getAllOrderSummaries(filters: OrderFilters = {}): Observable<OrderSummary[]> {
    let params = new HttpParams();

    // Solo se envían los filtros que tienen valor. En los ids se compara
    // con null para no ignorar un id válido.
    if (filters.from) params = params.set('from', filters.from);
    if (filters.to) params = params.set('to', filters.to);
    if (filters.productId != null)
      params = params.set('productId', filters.productId);
    if (filters.orderId != null)
      params = params.set('orderId', filters.orderId);

    return this.http.get<OrderSummary[]>(`${this.apiUrl}/all`, { params });
  }

  // =========================
  // Cambios de estado
  // =========================

  // Confirma un pedido pendiente (PENDING -> CONFIRMED).
  confirmOrder(orderId: number): Observable<Order> {
    return this.http.put<Order>(`${this.apiUrl}/${orderId}/confirm`, {});
  }

  // Pasa el pedido a preparación (CONFIRMED -> PREPARING).
  // Queda a cargo del operador con sesión iniciada.
  cookOrder(orderId: number): Observable<Order> {
    return this.http.put<Order>(`${this.apiUrl}/${orderId}/cook`, {});
  }

  // Despacha el pedido y le asigna un domiciliario (PREPARING -> ON_THE_WAY).
  sendOrder(orderId: number, deliveryPersonId: number): Observable<Order> {
    return this.http.put<Order>(
      `${this.apiUrl}/${orderId}/send/${deliveryPersonId}`,
      {},
    );
  }

  // Marca el pedido como entregado (ON_THE_WAY -> DELIVERED).
  deliverOrder(orderId: number): Observable<Order> {
    return this.http.put<Order>(`${this.apiUrl}/${orderId}/deliver`, {});
  }

  // Cancela un pedido. El back verifica que sea del cliente con sesión iniciada.
  cancelOrder(orderId: number): Observable<Order> {
    return this.http.put<Order>(`${this.apiUrl}/${orderId}/cancel`, {});
  }
}