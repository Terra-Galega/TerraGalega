// Estado del último intento de pago de la suscripción (SubscriptionDTO.paymentState)
export type SubscriptionPaymentState = 'PENDING' | 'PAID' | 'EXPIRED';

// Respuesta de GET /subscriptions/status y POST /subscriptions/start.
export interface SubscriptionStatus {
  subscribed: boolean;
  pointsBalance: number;
  pointValueCop: number;
  // Fecha de vencimiento (ISO, sin zona horaria). null = nunca se ha suscrito.
  subscriptionEndsAt: string | null;
  // Días que le quedan, redondeados hacia arriba (0 si es freemium)
  daysLeft: number;
  // true si puede pagar ahora (es decir, si no tiene un pago pendiente y no está suscrito)
  canSubscribe: boolean;
  // Vienen del back: no hay segunda constante en el front
  priceCop: number;
  durationDays: number;
  // Estado del último pago (null si nunca ha pagado)
  paymentState: SubscriptionPaymentState | null;
  // Página de Stripe. Solo viene mientras el último pago está PENDING.
  checkoutUrl: string | null;
}