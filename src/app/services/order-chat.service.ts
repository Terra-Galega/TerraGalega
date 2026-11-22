import { Injectable, inject } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';
import { API_URL } from '../api-url';
import { OrderChat, OrderChatMessage, OrderChatSummary } from '../models/order-chat';

// Chat entre cliente y operador sobre un pedido
@Injectable({ providedIn: 'root' })
export class OrderChatService {
  private http = inject(HttpClient);
  private apiUrl = `${API_URL}/order-chat`;

  // Si se manda "after", solo llegan los mensajes nuevos
  getMessages(orderId: number, after?: number): Observable<OrderChat> {
    let params = new HttpParams();
    if (after != null) {
      params = params.set('after', after);
    }
    return this.http.get<OrderChat>(`${this.apiUrl}/${orderId}/messages`, { params });
  }

  sendMessage(orderId: number, content: string): Observable<OrderChatMessage> {
    return this.http.post<OrderChatMessage>(`${this.apiUrl}/${orderId}/messages`, { content });
  }

  // Chats activos (pedidos sin terminar que ya tienen mensajes)
  getActive(): Observable<OrderChatSummary[]> {
    return this.http.get<OrderChatSummary[]>(`${this.apiUrl}/active`);
  }
}