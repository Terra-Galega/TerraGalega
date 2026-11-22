export interface PointsQuote {
  subscribed: boolean;
  balance: number;
  required: number;
  total: number;
  canPay: boolean;
}

export interface PointsMovement {
  orderId: number;
  kind: 'EARNED' | 'SPENT' | 'RETURNED';
  amount: number;
  createdAt: string;
}

export interface CheckoutItem {
  productId: number;
  quantity: number;
  addOnIds: number[];
}

export interface CheckoutRequest {
  address: string;
  items: CheckoutItem[];
  couponCode?: string | null;
}

// Valor fijo del domicilio (el backend cobra el mismo valor al crear el pedido)
export const DELIVERY_FEE = 6000;