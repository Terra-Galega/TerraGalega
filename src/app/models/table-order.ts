// Pedidos de mesa (los registra el mesero en la app; la cocina los prepara aquí)

export type TableOrderStatus = 'PENDING' | 'IN_PROGRESS' | 'DONE' | 'CANCELLED';

// La cocina avanza cada plato solo hacia delante: PENDING -> PREPARING -> READY
export type OrderItemStatus = 'PENDING' | 'PREPARING' | 'READY';

export interface TableOrderItem {
  id: number;
  productId: number;
  productName: string;
  quantity: number;
  notes: string | null;
  // Nombres de los adicionales del plato
  addOns: string[];
  status: OrderItemStatus;
}

export interface TableOrder {
  id: number;
  tableId: number;
  floor: number;
  waiterId: number;
  waiterName: string;
  startTime: string;
  endTime: string | null;
  status: TableOrderStatus;
  // Todos los platos listos: esperando en cocina a que el mesero lo recoja
  ready: boolean;
  cancellable: boolean;
  items: TableOrderItem[];
}

// Respuesta de GET /table-orders/kitchen: pedidos en curso agrupados por mesa,
// en el orden de llegada de su pedido más antiguo
export interface KitchenTable {
  tableId: number;
  floor: number;
  orders: TableOrder[];
}

// ---------- Panel del admin: órdenes en punto físico ----------

export type TablePaymentMethod = 'CASH' | 'CARD';

// Plato con los precios del momento del pedido. subtotal = (unitPrice + addOnsPrice) × quantity
export interface AdminTableOrderItem {
  id: number;
  productId: number;
  productName: string;
  category: string | null;
  quantity: number;
  unitPrice: number;
  addOns: string[];
  addOnsPrice: number;
  subtotal: number;
  notes: string | null;
  status: OrderItemStatus;
}

// Respuesta de GET /table-orders/admin
export interface AdminTableOrder {
  id: number;
  tableId: number;
  floor: number;
  waiterId: number;
  waiterName: string;
  startTime: string;
  endTime: string | null;
  status: TableOrderStatus;
  // Entregado y aún sin cobrar
  awaitingPayment: boolean;
  paymentMethod: TablePaymentMethod | null;
  paidAt: string | null;
  units: number;
  total: number;
  items: AdminTableOrderItem[];
}

// Filtros que resuelve el back (todos opcionales; fechas en formato yyyy-MM-dd)
export interface AdminTableOrderFilters {
  from?: string;
  to?: string;
  productId?: number | null;
  orderId?: number | null;
}

// Estado visible de un pedido de mesa en el panel
export type TableOrderStage = 'kitchen' | 'unpaid' | 'paid' | 'cancelled';

export function tableOrderStage(o: AdminTableOrder): TableOrderStage {
  if (o.status === 'CANCELLED') return 'cancelled';
  if (o.status === 'DONE') return o.paidAt ? 'paid' : 'unpaid';
  return 'kitchen';
}

export const TABLE_STAGE_LABEL: Record<TableOrderStage, string> = {
  kitchen: 'En cocina',
  unpaid: 'Por cobrar',
  paid: 'Pagado',
  cancelled: 'Cancelado',
};

export const PAYMENT_LABEL: Record<TablePaymentMethod, string> = {
  CASH: 'Efectivo',
  CARD: 'Tarjeta',
};
