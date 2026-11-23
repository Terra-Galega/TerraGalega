// Temática de la reserva (mismos valores que el enum ReservationTheme del back).
export type ReservationTheme =
  | 'GRADUATION'
  | 'PAIRING'
  | 'GENDER_REVEAL'
  | 'BACHELOR_PARTY'
  | 'BIRTHDAY'
  | 'LOVE'
  | 'BUSINESS'
  | 'NONE';

// Temáticas con el texto que ve el cliente, en el orden en que se ofrecen.
export const RESERVATION_THEMES: { value: ReservationTheme; label: string }[] = [
  { value: 'NONE', label: 'Ninguna' },
  { value: 'GRADUATION', label: 'Graduación' },
  { value: 'PAIRING', label: 'Maridaje' },
  { value: 'GENDER_REVEAL', label: 'Revelación de género' },
  { value: 'BACHELOR_PARTY', label: 'Despedida de soltero/a' },
  { value: 'BIRTHDAY', label: 'Cumpleaños' },
  { value: 'LOVE', label: 'Amor' },
  { value: 'BUSINESS', label: 'Negocios' },
];

// Estado del pago del anticipo:
// PENDING: la reserva espera el pago en Stripe (la mesa queda apartada).
// PAID: anticipo pagado, reserva confirmada.
// EXPIRED: la sesión de pago caducó; la reserva ya no existe y la mesa se liberó.
export type ReservationPaymentState = 'PENDING' | 'PAID' | 'EXPIRED';

// Reserva del cliente con sesión iniciada (llega de /clients/me/reservation).
export interface Reservation {
  id: number;
  clientId: number;
  name: string;
  // Número de la mesa del plano (101, 233...) y su piso
  tableId: number;
  floor: number;
  // AAAA-MM-DD
  date: string;
  // HH:mm:ss
  time: string;
  seats: number;
  theme: ReservationTheme;
  // Anticipo no reembolsable (lo define el back: 50.000)
  depositAmount: number;
  paymentState: ReservationPaymentState;
  // Página de Stripe donde se paga el anticipo (solo mientras está PENDING)
  checkoutUrl: string | null;
}

// Datos que se envían al crear o editar (el id y el cliente los pone el back).
export interface ReservationInput {
  name: string;
  tableId: number;
  date: string;
  time: string;
  seats: number;
  theme: ReservationTheme;
  // Solo al crear: el cliente acepta pagar el anticipo
  depositAccepted?: boolean;
}

// Estado de una reserva activa vista por el operador:
// UPCOMING: aún no es la hora. WAITING: ya es la hora y se espera al cliente.
// SEATED: el cliente ya llegó y está en la mesa. NO_SHOW: pasó el margen de espera sin llegar.
export type OperatorReservationStatus = 'UPCOMING' | 'WAITING' | 'SEATED' | 'NO_SHOW';

// Reserva activa en el panel del operador (llega de GET /reservations/active).
export interface OperatorReservation {
  id: number;
  name: string;
  clientName: string;
  clientPhone: string | null;
  tableId: number;
  floor: number;
  // AAAA-MM-DD
  date: string;
  // HH:mm:ss
  time: string;
  seats: number;
  theme: ReservationTheme;
  status: OperatorReservationStatus;
}