import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { API_URL } from './auth.service';
import { OperatorDelivery } from '../models/delivery-tracking';

@Injectable({ providedIn: 'root' })
export class DeliveryTrackingService {
  private http = inject(HttpClient);

  getActiveDeliveries() {
    return this.http.get<OperatorDelivery[]>(
      `${API_URL}/delivery/operator/active`,
    );
  }
}
