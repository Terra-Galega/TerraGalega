export type ChatRole = 'CLIENT' | 'OPERATOR';

export interface OrderChatMessage {
  id: number;
  senderRole: ChatRole;
  senderName: string;
  content: string;
  createdAt: string;
}

export interface OrderChat {
  orderId: number;
  orderStatus: string;
  open: boolean;
  messages: OrderChatMessage[];
}
// Pedido activo que ya tiene mensajes
export interface OrderChatSummary {
  orderId: number;
  orderStatus: string;
  lastMessageId: number;
  lastSenderRole: ChatRole;
  lastMessageAt: string;
}