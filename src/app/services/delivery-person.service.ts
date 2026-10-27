import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable, Subject } from 'rxjs';
import {
  DeliveryPerson,
  CreateDeliveryPerson,
  DeliveryPersonOption,
} from '../models/deliveryPerson';

// Servicio encargado de gestionar los domiciliarios y comunicarse con el backend.
@Injectable({ providedIn: 'root' })
export class DeliveryPersonService {
  // URL base de los endpoints de domiciliarios en Spring Boot.
  private apiUrl = 'http://localhost:8080/delivery-persons';

  // Avisa a la lista del admin que debe volver a cargar los domiciliarios
  private refreshSubject = new Subject<void>();

  // Observable al que se suscribe la lista del admin para recargarse.
  refresh$ = this.refreshSubject.asObservable();

  // Emite una notificación para solicitar la actualización de los datos.
  refresh() {
    this.refreshSubject.next();
  }

  // Inyecta el cliente HTTP para realizar solicitudes al backend.
  constructor(private http: HttpClient) {}

  // =========================
  // Consultas
  // =========================

  // Obtiene la lista completa de domiciliarios (panel de admin).
  getDeliveryPersons(): Observable<DeliveryPerson[]> {
    return this.http.get<DeliveryPerson[]>(this.apiUrl);
  }

  // Solo activos y que no estén entregando un pedido (para el modal de envío)
  getAvailableDeliveryPersons(): Observable<DeliveryPersonOption[]> {
    return this.http.get<DeliveryPersonOption[]>(`${this.apiUrl}/available`);
  }

  // =========================
  // CRUD conectado a Spring Boot
  // =========================

  // Registra un nuevo domiciliario en el backend.
  addDeliveryPerson(
    deliveryPerson: CreateDeliveryPerson,
  ): Observable<DeliveryPerson> {
    return this.http.post<DeliveryPerson>(this.apiUrl, deliveryPerson);
  }

  // Actualiza los datos de un domiciliario existente.
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

  // Elimina un domiciliario por su identificador.
  deleteDeliveryPerson(id: number): Observable<void> {
    return this.http.delete<void>(`${this.apiUrl}/${id}`);
  }

  // Cambia la contraseña del domiciliario. Se envía la actual para que el
  // backend confirme que quien la cambia es quien dice ser.
  changePassword(
    id: number,
    currentPassword: string,
    newPassword: string,
  ): Observable<void> {
    return this.http.put<void>(`${this.apiUrl}/${id}/password`, {
      currentPassword,
      newPassword,
    });
  }
}