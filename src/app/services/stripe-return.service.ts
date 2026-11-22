import { Injectable } from '@angular/core';

// Qué se estaba pagando al salir hacia Stripe
export type StripeRedirectKind = 'order' | 'reservation';

export interface StripeRedirect {
  kind: StripeRedirectKind;
  // orderId del pedido o id de la reserva
  id: number;
}

const KEY = 'terra.stripeRedirect';

// Recuerda que el navegador salió hacia la pasarela de Stripe.
//
// Stripe siempre vuelve a una página de resultado (/checkout/result o
// /reservations/result), que borra esta marca. Si el cliente vuelve con el botón
// "Atrás" del navegador, llega a la página desde la que pagó con la marca
// todavía guardada: así esa página sabe que el pago no se terminó, desbloquea su
// botón y muestra el aviso para volver a intentarlo.
// sessionStorage: dura lo que la pestaña, y el "Atrás" vuelve a la misma pestaña.
@Injectable({ providedIn: 'root' })
export class StripeReturnService {
  // Se llama justo antes de cambiar window.location a la página de Stripe
  mark(kind: StripeRedirectKind, id: number): void {
    try {
      sessionStorage.setItem(KEY, JSON.stringify({ kind, id }));
    } catch {
      // Sin almacenamiento solo se pierde el aviso al volver con "Atrás"
    }
  }

  // Devuelve la marca si es de ese tipo y la borra: el aviso se muestra una sola vez
  take(kind: StripeRedirectKind): StripeRedirect | null {
    try {
      const raw = sessionStorage.getItem(KEY);
      const saved = raw ? (JSON.parse(raw) as StripeRedirect) : null;
      if (!saved || saved.kind !== kind || !Number.isInteger(saved.id) || saved.id <= 0) {
        return null;
      }
      sessionStorage.removeItem(KEY);
      return saved;
    } catch {
      return null;
    }
  }

  // Las páginas de resultado la borran: a ellas sí se llega volviendo de Stripe
  clear(): void {
    try {
      sessionStorage.removeItem(KEY);
    } catch {
      // nada que borrar
    }
  }
}
