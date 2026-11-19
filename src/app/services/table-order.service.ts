import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';

import { API_URL } from '../api-url';
import { KitchenTable, OrderItemStatus, TableOrder } from '../models/table-order';

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
}
