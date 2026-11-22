import { Injectable, inject } from '@angular/core';
import { StripeReturnService } from './stripe-return.service';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';
import { Reservation, ReservationInput } from '../models/reservation';
import { API_URL } from '../api-url';

// Reserva del cliente con sesión iniciada (el back lo identifica por el token).
// Cada cliente tiene como máximo una reserva (relación 1 a 1) y para crearla debe
// pagar el anticipo en la pasarela de pago (Stripe Checkout).
@Injectable({
  providedIn: 'root',
})
export class ReservationService {
  private apiUrl = `${API_URL}/clients/me/reservation`;
  private stripeReturn = inject(StripeReturnService);

  constructor(private http: HttpClient) {}

  // Reserva del cliente; el back responde 404 si todavía no tiene.
  getReservation(): Observable<Reservation> {
    return this.http.get<Reservation>(this.apiUrl);
  }

  // Crea la reserva en estado PENDING (debe ir depositAccepted: true). La respuesta
  // trae checkoutUrl: la página de Stripe donde se paga el anticipo.
  createReservation(data: ReservationInput): Observable<Reservation> {
    return this.http.post<Reservation>(this.apiUrl, data);
  }

  // Estado del pago del anticipo. El back consulta a Stripe, así que sirve aunque
  // el webhook todavía no haya llegado.
  getPayment(): Observable<Reservation> {
    return this.http.get<Reservation>(`${this.apiUrl}/payment`);
  }

  // Edita la reserva existente (solo si el anticipo ya está pagado).
  updateReservation(data: ReservationInput): Observable<Reservation> {
    return this.http.put<Reservation>(this.apiUrl, data);
  }

  // Elimina la reserva. El anticipo no se reembolsa; si el pago seguía pendiente,
  // se cierra la sesión de Stripe y no se cobra nada.
  deleteReservation(): Observable<void> {
    return this.http.delete<void>(this.apiUrl);
  }

  // Números de las mesas ya reservadas por otros clientes en esa fecha y hora.
  getTakenTables(date: string, time: string): Observable<number[]> {
    const params = new HttpParams().set('date', date).set('time', time);
    return this.http.get<number[]>(`${this.apiUrl}/taken`, { params });
  }

  // Lleva al cliente a la página de pago de Stripe.
  redirectToStripe(reservation: Reservation): void {
    if (reservation.checkoutUrl) {
      // Si vuelve con "Atrás" sin pagar, la página de reservas lo sabrá
      this.stripeReturn.mark('reservation', reservation.id);
      window.location.href = reservation.checkoutUrl;
    }
  }
}
