import { Component, inject } from '@angular/core';
import { DatePipe, DecimalPipe } from '@angular/common';
import { OrderService } from '../../../../services/order.service';
import { Order, ORDER_STATUS_LABELS } from '../../../../models/order';

@Component({
  selector: 'app-admin-orders',
  imports: [DatePipe, DecimalPipe],
  templateUrl: './admin-orders.component.html',
  styleUrl: './admin-orders.component.scss',
})
export class AdminOrdersComponent {
  private orderService = inject(OrderService);

  orders: Order[] = [];
  statusLabels = ORDER_STATUS_LABELS;

  constructor() {
    this.loadOrders();
    this.orderService.refresh$.subscribe(() => this.loadOrders());
  }

  loadOrders() {
    this.orderService.getOrders().subscribe({
      next: (orders) => (this.orders = orders),
      error: (error) => console.error('Error cargando pedidos:', error),
    });
  }
}