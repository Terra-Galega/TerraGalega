import { Product } from './product';

export type OrderStatus =
  | 'PENDING'
  | 'CONFIRMED'
  | 'PREPARING'
  | 'ON_THE_WAY'
  | 'DELIVERED'
  | 'CANCELLED';

export const ORDER_STATUS_LABELS: Record<OrderStatus, string> = {
  PENDING: 'Pendiente',
  CONFIRMED: 'Confirmado',
  PREPARING: 'Preparando',
  ON_THE_WAY: 'En camino',
  DELIVERED: 'Entregado',
  CANCELLED: 'Cancelado',
};

export interface OrderAddOn {
  id: number;
  name: string;
  price: number;
}

export interface OrderDetail {
  id: number;
  product: Product;
  quantity: number;
  unitPrice: number;
  addOns: OrderAddOn[];
}

export interface Order {
  id: number;
  client: {
    id: number;
    name: string;
    lastName: string;
    email: string;
    phone: string;
  } | null;
  deliveryPerson?: {
    id: number;
    name: string;
    lastName: string;
    phone: string;
  } | null;
  status: OrderStatus;
  createdAt: string;
  deliveredAt: string | null;
  address: string;
  details: OrderDetail[];
  total: number;
}