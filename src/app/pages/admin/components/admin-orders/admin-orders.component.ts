import { Component, inject } from '@angular/core';
import { DatePipe, DecimalPipe } from '@angular/common';
import { OrderService } from '../../../../services/order.service';
import { OrderSummary, ORDER_STATUS_LABELS } from '../../../../models/order';

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
}