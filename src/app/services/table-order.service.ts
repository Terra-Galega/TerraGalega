import { Injectable, inject } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';

import { API_URL } from '../api-url';
import {
  AdminTableOrder,
  AdminTableOrderFilters,
  KitchenTable,
  OrderItemStatus,
  TableOrder,
} from '../models/table-order';

// Pantalla de cocina (panel del operador): pedidos de mesa en curso
@Injectable({ providedIn: 'root' })
export class TableOrderService {
  private http = inject(HttpClient);
  private apiUrl = `${API_URL}/table-orders`;

  // Pedidos en curso agrupados por mesa, del más antiguo al más nuevo
  kitchen(): Observable<KitchenTable[]> {
    return this.http.get<KitchenTable[]>(`${this.apiUrl}/kitchen`);
  }

  // Avanza un plato a PREPARING o READY; devuelve el pedido actualizado
  updateItemStatus(orderId: number, itemId: number, status: OrderItemStatus): Observable<TableOrder> {
    return this.http.put<TableOrder>(`${this.apiUrl}/${orderId}/items/${itemId}/status`, { status });
  }

  // Cocina: pasa todos los platos del pedido a la vez a "en preparación" o a "listo"
  updateOrderStatus(orderId: number, status: OrderItemStatus): Observable<TableOrder> {
    return this.http.put<TableOrder>(`${this.apiUrl}/${orderId}/status`, { status });
  }

  // Admin: órdenes en punto físico con sus precios, del más reciente al más antiguo.
  // Solo se envían los filtros que tienen valor.
  adminOrders(filters: AdminTableOrderFilters = {}): Observable<AdminTableOrder[]> {
    let params = new HttpParams();
    if (filters.from) params = params.set('from', filters.from);
    if (filters.to) params = params.set('to', filters.to);
    if (filters.productId != null) params = params.set('productId', filters.productId);
    if (filters.orderId != null) params = params.set('orderId', filters.orderId);
    return this.http.get<AdminTableOrder[]>(`${this.apiUrl}/admin`, { params });
  }
}
