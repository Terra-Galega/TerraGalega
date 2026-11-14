import { CheckoutRequest } from './checkout';

// Estados del pago en línea que devuelve el back (PaymentDTO.state)
export type PaymentState =
  | 'PENDING'
  | 'PAID'
  | 'EXPIRED'
  | 'REFUND_PENDING'
  | 'REFUNDED'
  | 'REFUND_FAILED';

// Cuerpo de POST /payments/start
export interface StartPaymentRequest {
  // UUID del intento. Si se reenvía el mismo, el back devuelve el mismo pago
  // en vez de crear otro pedido (por ejemplo, tras un fallo de red).
  requestId: string;
  checkout: CheckoutRequest;
}

// Respuesta de /payments/*
export interface Payment {
  orderId: number;
  state: PaymentState;
  // Página de Stripe donde el cliente paga. Solo viene mientras está PENDING.
  checkoutUrl: string | null;
  // Importe en centavos (COP × 100)
  amountInCents: number;
  currency: string;
}

// Lo que hay que hacer cuando el pago se apruebe. Se guarda en sessionStorage
// porque al ir a Stripe la app se descarga y se pierde lo que había en memoria.
export interface PendingPayment {
  orderId: number;
  address: string;
  saveAddress: boolean;
  addressName: string;
}
