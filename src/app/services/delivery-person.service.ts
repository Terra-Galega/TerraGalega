import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { DeliveryPerson } from '../models/deliveryPerson';

@Injectable({ providedIn: 'root' })
export class DeliveryPersonService {
  private apiUrl = 'http://localhost:8080/delivery-persons';

  constructor(private http: HttpClient) {}

  getAvailableDeliveryPersons(): Observable<DeliveryPerson[]> {
    return this.http.get<DeliveryPerson[]>(`${this.apiUrl}/available`);
  }
}