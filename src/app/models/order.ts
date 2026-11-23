export type OrderStatus =
  | 'PENDING'
  | 'CONFIRMED'
  | 'PREPARING'
  | 'ON_THE_WAY'
  | 'DELIVERED'
  | 'CANCELLED';

// Texto en español de cada estado del pedido
export function orderStatusLabel(status: OrderStatus): string {
  switch (status) {
    case 'PENDING':
      return 'Pendiente';
    case 'CONFIRMED':
      return 'Confirmado';
    case 'PREPARING':
      return 'Preparando';
    case 'ON_THE_WAY':
      return 'En camino';
    case 'DELIVERED':
      return 'Entregado';
    case 'CANCELLED':
      return 'Cancelado';
  }
}

export interface OrderAddOn {
  id: number;
  name: string;
  price: number;
}
// Lo único que el pedido necesita de cada producto
export interface OrderProduct {
  id: number;
  name: string;
  imageUrl?: string | null;
}

export interface OrderDetail {
  id: number;
  product: OrderProduct;
  quantity: number;
  unitPrice: number;
  addOns: OrderAddOn[];
}
export interface Order {
  id: number;
  client?: {
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
  // Operador que maneja el pedido; no viene hasta que empieza a prepararse
  operatorId?: number | null;
  status: OrderStatus;
  createdAt: string;
  deliveredAt: string | null;
  address: string;
  details: OrderDetail[];
  pointsSpent?: number | null;
  // Total que paga el cliente: productos + adicionales + domicilio − descuento
  total: number;
  // Solo vienen si el pedido usó un cupón: valor descontado y código usado
  discount?: number;
  couponCode?: string;
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