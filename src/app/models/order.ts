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
export interface OrderSummary {
  id: number;
  status: OrderStatus;
  createdAt: string;
  address: string;
  clientName: string | null;
  deliveryPersonName: string | null;
  total: number;
}

export interface GroupedAddOn {
  id: number;
  name: string;
  count: number;
}

// Agrupa adicionales repetidos: [queso, queso, salsa] -> [{queso x2}, {salsa x1}]
export function groupAddOns(
  addOns: { id: number; name: string }[],
): GroupedAddOn[] {
  const map = new Map<number, GroupedAddOn>();
  for (const a of addOns) {
    const existing = map.get(a.id);
    if (existing) {
      existing.count++;
    } else {
      map.set(a.id, { id: a.id, name: a.name, count: 1 });
    }
  }
  return [...map.values()];
}
