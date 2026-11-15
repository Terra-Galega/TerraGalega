import { API_URL } from '../api-url';
import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { SubscriptionStatus } from '../models/subscription';

@Injectable({ providedIn: 'root' })
export class SubscriptionService {
  private apiUrl = `${API_URL}/subscriptions`;

  constructor(private http: HttpClient) {}

  // Estado actual. El back consulta a Stripe si hay un pago pendiente.
  status(): Observable<SubscriptionStatus> {
    return this.http.get<SubscriptionStatus>(`${this.apiUrl}/status`);
  }

  // Abre el pago (o reutiliza el abierto). Si aún no puede pagar, el back responde 409.
  start(): Observable<SubscriptionStatus> {
    return this.http.post<SubscriptionStatus>(`${this.apiUrl}/start`, {});
  }

  // Lleva al cliente a la página de pago de Stripe
  redirectToStripe(status: SubscriptionStatus): void {
    if (status.checkoutUrl) {
      window.location.href = status.checkoutUrl;
    }
  }
}

// Fecha lista para mostrar, p. ej. "14 de diciembre de 2026".
// Se recortan los decimales del LocalDateTime de Java para que todos los navegadores lo lean igual.
export function formatSubscriptionDate(iso: string | null | undefined): string {
  if (!iso) return '';

  const date = new Date(iso.slice(0, 19));
  if (Number.isNaN(date.getTime())) return '';

  return date.toLocaleDateString('es-CO', {
    day: 'numeric',
    month: 'long',
    year: 'numeric',
  });
}