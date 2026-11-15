import { Reservation } from './reservation';

// Mensaje de la conversación tal como lo recibe el back
export interface ChatMessage {
  role: 'user' | 'model';
  text: string;
}

// Respuesta de POST /clients/me/chat
export interface ChatResponse {
  reply: string;
  // true si el asistente cambió el carrito: hay que recargarlo
  cartChanged: boolean;
  // Reserva creada o consultada en este mensaje (null si no hubo)
  reservation: Reservation | null;
}

// Mensaje como se pinta en el widget
export interface ChatBubble {
  role: 'user' | 'model';
  text: string;
  reservation?: Reservation | null;
  // true = error o mensaje que no se pudo enviar (no se manda en el historial)
  error?: boolean;
}