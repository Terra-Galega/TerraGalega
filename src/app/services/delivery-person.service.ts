import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable, Subject } from 'rxjs';
import { DeliveryPerson, CreateDeliveryPerson } from '../models/deliveryPerson';

@Injectable({ providedIn: 'root' })
export class DeliveryPersonService {
  private apiUrl = 'http://localhost:8080/delivery-persons';

  // Avisa a la lista del admin que debe volver a cargar los domiciliarios
  private refreshSubject = new Subject<void>();

  refresh$ = this.refreshSubject.asObservable();

  refresh() {
    this.refreshSubject.next();
  }

  constructor(private http: HttpClient) {}

  getDeliveryPersons(): Observable<DeliveryPerson[]> {
    return this.http.get<DeliveryPerson[]>(this.apiUrl);
  }

  // Solo activos y que no estén entregando un pedido (para el modal de envío)
  getAvailableDeliveryPersons(): Observable<DeliveryPerson[]> {
    return this.http.get<DeliveryPerson[]>(`${this.apiUrl}/available`);
  }

  addDeliveryPerson(
    deliveryPerson: CreateDeliveryPerson,
  ): Observable<DeliveryPerson> {
    return this.http.post<DeliveryPerson>(this.apiUrl, deliveryPerson);
  }

  updateDeliveryPerson(
    deliveryPerson: DeliveryPerson,
  ): Observable<DeliveryPerson> {
    return this.http.put<DeliveryPerson>(
      `${this.apiUrl}/${deliveryPerson.id}`,
      deliveryPerson,
    );
  }

  // Activa o desactiva al domiciliario
  toggleDeliveryPersonActive(id: number): Observable<DeliveryPerson> {
    return this.http.put<DeliveryPerson>(`${this.apiUrl}/${id}/toggle`, {});
  }

  deleteDeliveryPerson(id: number): Observable<void> {
    return this.http.delete<void>(`${this.apiUrl}/${id}`);
  }
}
