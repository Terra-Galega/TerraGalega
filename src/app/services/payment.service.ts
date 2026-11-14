import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import {
  Payment,
  PendingPayment,
  StartPaymentRequest,
} from '../models/payment';

const PENDING_KEY = 'terra.pendingPayment';

// Pagos con tarjeta mediante Stripe Checkout. Los datos de la tarjeta se
// escriben en la página de Stripe: nunca pasan por esta app ni por el back.
@Injectable({ providedIn: 'root' })
export class PaymentService {
  private apiUrl = 'http://localhost:8080/payments';

  constructor(private http: HttpClient) {}

  // Crea el pedido (oculto hasta que se pague) y la sesión de Stripe
  start(request: StartPaymentRequest): Observable<Payment> {
    return this.http.post<Payment>(`${this.apiUrl}/start`, request);
  }

  // Estado actual del pago. El back también consulta a Stripe, así que sirve
  // aunque el webhook todavía no haya llegado.
  status(orderId: number): Observable<Payment> {
    return this.http.get<Payment>(`${this.apiUrl}/${orderId}`);
  }

  // Cancela el pago: si aún no se pagó, cierra la sesión de Stripe;
  // si ya se pagó y el restaurante no lo confirmó, lo reembolsa.
  cancel(orderId: number): Observable<Payment> {
    return this.http.post<Payment>(`${this.apiUrl}/${orderId}/cancel`, {});
  }

  // Lleva al cliente a la página de pago de Stripe
  redirectToStripe(payment: Payment): void {
    if (payment.checkoutUrl) {
      window.location.href = payment.checkoutUrl;
    }
  }

  // ---- Lo pendiente de hacer al volver de Stripe (sessionStorage) ----

  savePending(pending: PendingPayment): void {
    try {
      sessionStorage.setItem(PENDING_KEY, JSON.stringify(pending));
    } catch {
      // Sin almacenamiento solo se pierde guardar la dirección en el perfil
    }
  }

  getPending(orderId: number): PendingPayment | null {
    try {
      const raw = sessionStorage.getItem(PENDING_KEY);
      const pending = raw ? (JSON.parse(raw) as PendingPayment) : null;
      return pending?.orderId === orderId ? pending : null;
    } catch {
      return null;
    }
  }

  clearPending(): void {
    try {
      sessionStorage.removeItem(PENDING_KEY);
    } catch {
      // nada que limpiar
    }
  }
}
