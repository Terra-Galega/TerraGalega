import { Component, inject } from '@angular/core';
import { DatePipe, DecimalPipe } from '@angular/common';
import { TableOrderService } from '../../../../services/table-order.service';
import { ProductService } from '../../../../services/product.service';
import { ProductOption } from '../../../../models/product';
import {
  AdminTableOrder,
  PAYMENT_LABEL,
  TABLE_STAGE_LABEL,
  TableOrderStage,
  tableOrderStage,
} from '../../../../models/table-order';
import { getErrorMessage } from '../../../../utils/http-error';

// Filtro rápido por estado: todos o una de las etapas del pedido de mesa
type StageFilter = 'all' | TableOrderStage;

// Órdenes en punto físico (las que registran los meseros desde la app): se filtran
// por fechas, producto, número de pedido, mesero, mesa y estado, y cada una se
// abre para ver sus platos, precios, cobro y tiempos.
@Component({
  selector: 'app-admin-table-orders',
  imports: [DatePipe, DecimalPipe],
  templateUrl: './admin-table-orders.component.html',
})
export class AdminTableOrdersComponent {
  private tableOrderService = inject(TableOrderService);
  private productService = inject(ProductService);

  orders: AdminTableOrder[] = [];
  products: ProductOption[] = [];
  loading = true;
  errorMessage = '';

  // Filtros que se resuelven en el back
  dateFrom = '';
  dateTo = '';
  productId: number | null = null;
  orderNumber = '';

  // Filtros que se resuelven aquí, sobre lo que llegó del back
  waiterId: number | null = null;
  tableId: number | null = null;
  stage: StageFilter = 'all';

  // Pedido abierto para ver su detalle
  openId: number | null = null;

  readonly stageLabel = TABLE_STAGE_LABEL;
  readonly paymentLabel = PAYMENT_LABEL;
  readonly stageOf = tableOrderStage;

  readonly stageOptions: { value: StageFilter; label: string }[] = [
    { value: 'all', label: 'Todos' },
    { value: 'kitchen', label: 'En cocina' },
    { value: 'unpaid', label: 'Por cobrar' },
    { value: 'paid', label: 'Pagados' },
    { value: 'cancelled', label: 'Cancelados' },
  ];

  // Colores de la etiqueta de cada estado
  readonly stageStyle: Record<TableOrderStage, { bg: string; color: string }> = {
    kitchen: { bg: 'rgba(178, 87, 31, 0.14)', color: '#b2571f' },
    unpaid: { bg: 'rgba(217, 154, 33, 0.2)', color: '#9a6a05' },
    paid: { bg: 'rgba(85, 97, 65, 0.16)', color: '#556141' },
    cancelled: { bg: 'rgba(138, 34, 34, 0.12)', color: '#8a2222' },
  };

  constructor() {
    this.loadOrders();
    this.productService.getProductOptions().subscribe({
      next: (products) =>
        (this.products = [...products].sort((a, b) =>
          a.name.localeCompare(b.name),
        )),
      error: (error) => console.error('Error cargando productos:', error),
    });
  }

  loadOrders() {
    const parsed = this.orderNumber.trim() ? Number(this.orderNumber) : null;
    this.loading = true;
    this.tableOrderService
      .adminOrders({
        from: this.dateFrom,
        to: this.dateTo,
        productId: this.productId,
        orderId: parsed !== null && !Number.isNaN(parsed) ? parsed : null,
      })
      .subscribe({
        next: (orders) => {
          this.orders = orders;
          this.errorMessage = '';
          this.loading = false;
        },
        error: (error) => {
          console.error('Error cargando órdenes en punto físico:', error);
          this.orders = [];
          this.loading = false;
          this.errorMessage = getErrorMessage(
            error,
            'No se pudieron cargar las órdenes en punto físico.',
          );
        },
      });
  }

  onDateFromChange(event: Event) {
    this.dateFrom = (event.target as HTMLInputElement).value;
    this.loadOrders();
  }

  onDateToChange(event: Event) {
    this.dateTo = (event.target as HTMLInputElement).value;
    this.loadOrders();
  }

  onProductChange(event: Event) {
    const value = (event.target as HTMLSelectElement).value;
    this.productId = value ? Number(value) : null;
    this.loadOrders();
  }

  onOrderNumberChange(event: Event) {
    this.orderNumber = (event.target as HTMLInputElement).value;
    this.loadOrders();
  }

  onWaiterChange(event: Event) {
    const value = (event.target as HTMLSelectElement).value;
    this.waiterId = value ? Number(value) : null;
  }

  onTableChange(event: Event) {
    const value = (event.target as HTMLSelectElement).value;
    this.tableId = value ? Number(value) : null;
  }

  get hasFilters(): boolean {
    return (
      !!this.dateFrom ||
      !!this.dateTo ||
      this.productId !== null ||
      !!this.orderNumber.trim() ||
      this.waiterId !== null ||
      this.tableId !== null
    );
  }

  clearFilters() {
    this.dateFrom = '';
    this.dateTo = '';
    this.productId = null;
    this.orderNumber = '';
    this.waiterId = null;
    this.tableId = null;
    this.loadOrders();
  }

  // Meseros y mesas que aparecen en lo traído, para los desplegables
  get waiters(): { id: number; name: string }[] {
    const map = new Map<number, string>();
    this.orders.forEach((o) => map.set(o.waiterId, o.waiterName));
    return [...map]
      .map(([id, name]) => ({ id, name }))
      .sort((a, b) => a.name.localeCompare(b.name));
  }

  get tables(): number[] {
    return [...new Set(this.orders.map((o) => o.tableId))].sort((a, b) => a - b);
  }

  get filteredOrders(): AdminTableOrder[] {
    return this.orders.filter(
      (o) =>
        (this.waiterId === null || o.waiterId === this.waiterId) &&
        (this.tableId === null || o.tableId === this.tableId) &&
        (this.stage === 'all' || tableOrderStage(o) === this.stage),
    );
  }

  // Resumen de lo que se ve (los cancelados no cuentan como venta)
  get summary() {
    const counted = this.filteredOrders.filter((o) => o.status !== 'CANCELLED');
    const sales = counted.reduce((s, o) => s + o.total, 0);
    const paid = counted.filter((o) => o.paidAt);
    return {
      count: this.filteredOrders.length,
      sales,
      average: counted.length ? sales / counted.length : 0,
      cash: paid.filter((o) => o.paymentMethod === 'CASH').reduce((s, o) => s + o.total, 0),
      card: paid.filter((o) => o.paymentMethod === 'CARD').reduce((s, o) => s + o.total, 0),
      unpaid: counted.filter((o) => o.awaitingPayment).reduce((s, o) => s + o.total, 0),
    };
  }

  toggle(order: AdminTableOrder) {
    this.openId = this.openId === order.id ? null : order.id;
  }

  // Minutos desde que se tomó el pedido hasta que se entregó o canceló
  // (o hasta ahora, si sigue en cocina)
  durationMin(order: AdminTableOrder): number {
    const start = new Date(order.startTime).getTime();
    const end = order.endTime ? new Date(order.endTime).getTime() : Date.now();
    return Math.max(0, Math.round((end - start) / 60_000));
  }

  itemStatusLabel(status: string): string {
    return status === 'READY' ? 'Listo' : status === 'PREPARING' ? 'En preparación' : 'En espera';
  }
}
