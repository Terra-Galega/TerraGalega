import { Component, inject } from '@angular/core';
import { DatePipe, DecimalPipe } from '@angular/common';
import { OrderService } from '../../../../services/order.service';
import { OrderSummary, ORDER_STATUS_LABELS } from '../../../../models/order';

type OrderFilter = 'all' | 'active' | 'completed';

@Component({
  selector: 'app-admin-orders',
  imports: [DatePipe, DecimalPipe],
  templateUrl: './admin-orders.component.html',
  styleUrl: './admin-orders.component.scss',
})
export class AdminOrdersComponent {
  private orderService = inject(OrderService);

  ordersSummary: OrderSummary[] = [];
  statusLabels = ORDER_STATUS_LABELS;

  // Filtro de la lista: todos, solo activos o solo completados
  filter: OrderFilter = 'all';

  readonly filterOptions: { value: OrderFilter; label: string }[] = [
    { value: 'all', label: 'Todos' },
    { value: 'active', label: 'Activos' },
    { value: 'completed', label: 'Completados' },
  ];

  constructor() {
    this.loadOrders();
    this.orderService.refresh$.subscribe(() => this.loadOrders());
  }

  loadOrders() {
    this.orderService.getAllOrderSummaries().subscribe({
      next: (orders) => (this.ordersSummary = orders),
      error: (error) => console.error('Error cargando pedidos:', error),
    });
  }

  // Un pedido está completado cuando se entregó o se canceló
  private isFinished(order: OrderSummary): boolean {
    return order.status === 'DELIVERED' || order.status === 'CANCELLED';
  }

  get filteredOrders(): OrderSummary[] {
    if (this.filter === 'all') {
      return this.ordersSummary;
    }

    return this.ordersSummary.filter((order) =>
      this.filter === 'completed'
        ? this.isFinished(order)
        : !this.isFinished(order),
    );
  }
}
