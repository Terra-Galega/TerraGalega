export interface CheckoutItem {
  productId: number;
  quantity: number;
  addOnIds: number[];
}

export interface CheckoutRequest {
  address: string;
  items: CheckoutItem[];
}
