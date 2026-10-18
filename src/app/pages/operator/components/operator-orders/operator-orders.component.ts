import { Component, EventEmitter, Input, Output, inject } from '@angular/core';
import { DatePipe, DecimalPipe } from '@angular/common';
import { finalize } from 'rxjs';
import { OrderService } from '../../../../services/order.service';
import { ButtonComponent } from '../../../../components/button/button.component';
import {
  Order,
  OrderDetail,
  OrderStatus,
  ORDER_STATUS_LABELS,
} from '../../../../models/order';

type OperatorAction = 'cook' | 'send' | 'deliver';

// What the operator can do next, depending on the order status
const NEXT_ACTION: Partial<Record<OrderStatus, OperatorAction>> = {
  PENDING: 'cook',
  PREPARING: 'send',
  ON_THE_WAY: 'deliver',
};

// Button text shown to the operator
const ACTION_LABELS: Record<OperatorAction, string> = {
  cook: 'Cocinar',
  send: 'Enviar',
  deliver: 'Completar',
};

const STATUS_COLORS: Record<OrderStatus, string> = {
  PENDING: '#d99a21',
  CONFIRMED: '#d99a21',
  PREPARING: '#b2571f',
  ON_THE_WAY: '#556141',
  DELIVERED: '#556141',
  CANCELLED: '#da5858',
};

@Component({
  selector: 'app-operator-orders',
  imports: [DatePipe, DecimalPipe, ButtonComponent],
  templateUrl: './operator-orders.component.html',
})
export class OperatorOrdersComponent {
  private orderService = inject(OrderService);

  @Input({ required: true }) operatorId!: number;

  // Sending an order needs a delivery person, so the parent opens the modal
  @Output() sendRequested = new EventEmitter<Order>();

  orders: Order[] = [];
  errorMessage = '';

  // Order with a request in flight, to avoid double clicks
  busyOrderId: number | null = null;

  statusLabels = ORDER_STATUS_LABELS;
  statusColors = STATUS_COLORS;

  constructor() {
    this.loadOrders();

    this.orderService.refresh$.subscribe(() => {
      this.loadOrders();
    });
  }

  loadOrders() {
    this.orderService.getActiveOrders().subscribe({
      next: (orders) => {
        this.orders = orders;
        this.errorMessage = '';
      },
      error: (error) => {
        console.error('Error loading active orders:', error);
        this.errorMessage = 'No se pudieron cargar los pedidos.';
      },
    });
  }

  onAction(order: Order) {
    const action = NEXT_ACTION[order.status];

    if (!action || this.busyOrderId !== null) {
      return;
    }

    if (action === 'send') {
      this.sendRequested.emit(order);
      return;
    }

    const request$ =
      action === 'cook'
        ? this.orderService.cookOrder(order.id, this.operatorId)
        : this.orderService.deliverOrder(order.id);

    this.errorMessage = '';
    this.busyOrderId = order.id;

    request$.pipe(finalize(() => (this.busyOrderId = null))).subscribe({
      next: () => this.orderService.refresh(),
      error: (error) => {
        console.error('Error updating order:', error);
        this.errorMessage =
          error.error?.message ?? 'No se pudo actualizar el pedido.';
      },
    });
  }

  actionLabel(order: Order): string | null {
    const action = NEXT_ACTION[order.status];
    return action ? ACTION_LABELS[action] : null;
  }

  clientName(order: Order): string {
    return order.client
      ? `${order.client.name} ${order.client.lastName}`
      : 'Cliente eliminado';
  }

  deliveryPersonName(order: Order): string {
    return order.deliveryPerson
      ? `${order.deliveryPerson.name} ${order.deliveryPerson.lastName}`
      : '—';
  }

  addOnNames(detail: OrderDetail): string {
    return detail.addOns.map((addOn) => addOn.name).join(', ');
  }
}