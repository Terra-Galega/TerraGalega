import { API_URL } from '../api-url';
import { AuthService } from './auth.service';
import { Injectable, inject } from '@angular/core';
import { StripeReturnService } from './stripe-return.service';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import {
  Payment,
  PendingPayment,
  StartPaymentRequest,
  InvoiceLink,
} from '../models/payment';

const PENDING_KEY = 'terra.pendingPayment';

// Pagos con tarjeta mediante Stripe Checkout. Los datos de la tarjeta se
// escriben en la página de Stripe: nunca pasan por esta app ni por el back.
@Injectable({ providedIn: 'root' })
export class PaymentService {
  private auth = inject(AuthService);
  private apiUrl = `${API_URL}/payments`;
  private stripeReturn = inject(StripeReturnService);

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

  // El backend comprueba el estado bajo bloqueo antes de cerrar la sesión.
  cancelPending(orderId: number): Observable<Payment> {
    return this.http.post<Payment>(`${this.apiUrl}/${orderId}/cancel-pending`, {});
  }

  // Lleva al cliente a la página de pago de Stripe
  redirectToStripe(payment: Payment): void {
    if (payment.checkoutUrl) {
      // Si vuelve con "Atrás" sin pagar, la página de confirmación lo sabrá
      this.stripeReturn.mark('order', payment.orderId);
      window.location.href = payment.checkoutUrl;
    }
  }

  private get clientKey(): string | null {
    const session = this.auth.session;
    return session?.type === 'client' ? PENDING_KEY + '.' + session.id : null;
  }

  savePending(pending: PendingPayment): void {
    const key = this.clientKey;
    if (!key) return;
    try {
      // Solo el identificador sobrevive al cierre de la pestaña.
      localStorage.setItem(key, String(pending.orderId));
    } catch { /* Se puede recuperar con los datos de esta pestaña. */ }
    try {
      sessionStorage.setItem(key, JSON.stringify(pending));
    } catch { /* La dirección es opcional para confirmar el pago. */ }
  }

  pendingOrderId(): number | null {
    const key = this.clientKey;
    if (!key) return null;
    try {
      const id = Number(localStorage.getItem(key));
      if (Number.isSafeInteger(id) && id > 0) return id;
    } catch { /* Continuar con el almacenamiento de la pestaña. */ }
    return this.getPending()?.orderId ?? null;
  }

  getPending(orderId?: number): PendingPayment | null {
    const key = this.clientKey;
    if (!key) return null;
    try {
      // PENDING_KEY sin sufijo se utilizaba en versiones anteriores.
      const raw = sessionStorage.getItem(key) ?? sessionStorage.getItem(PENDING_KEY);
      const data: PendingPayment | null = raw && JSON.parse(raw);
      if (!data || !Number.isSafeInteger(data.orderId) || data.orderId <= 0) return null;
      return orderId === undefined || data.orderId === orderId ? data : null;
    } catch {
      return null;
    }
  }

  clearPending(orderId: number): void {
    const key = this.clientKey;
    if (!key) return;
    try {
      if (localStorage.getItem(key) === String(orderId)) localStorage.removeItem(key);
    } catch { /* El almacenamiento puede estar deshabilitado. */ }
    for (const sessionKey of [key, PENDING_KEY]) {
      try {
        const raw = sessionStorage.getItem(sessionKey);
        if (raw && JSON.parse(raw)?.orderId === orderId) sessionStorage.removeItem(sessionKey);
      } catch { /* La otra clave aún puede ser válida. */ }
    }
  }

  // Enlace y QR de la factura de un pedido pagado
  invoiceLink(orderId: number): Observable<InvoiceLink> {
    return this.http.get<InvoiceLink>(`${API_URL}/invoices/${orderId}/link`);
  }
}
