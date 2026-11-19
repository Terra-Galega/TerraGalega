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
