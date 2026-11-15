import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { API_URL } from './auth.service';
import { ClientDelivery, OperatorDelivery } from '../models/delivery-tracking';

@Injectable({ providedIn: 'root' })
export class DeliveryTrackingService {
  private http = inject(HttpClient);

  getActiveDeliveries() {
    return this.http.get<OperatorDelivery[]>(
      `${API_URL}/delivery/operator/active`,
    );
  }

  // Pedidos en camino del cliente con sesión (incluye su PIN y la ubicación).
  getClientActive(clientId: number) {
    return this.http.get<ClientDelivery[]>(
      `${API_URL}/delivery/client/${clientId}/active`,
    );
  }
}
