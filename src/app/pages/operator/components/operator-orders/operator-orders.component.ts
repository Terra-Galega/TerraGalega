import { Component, EventEmitter, Input, Output, inject } from '@angular/core';
import { DatePipe, DecimalPipe } from '@angular/common';
import { finalize } from 'rxjs';
import { OrderService } from '../../../../services/order.service';
import { ButtonComponent } from '../../../../components/button/button.component';
import { OrderProgressComponent } from '../../../../components/order-progress/order-progress.component';
import {
  OrderStatus,
  OrderSummary,
  ORDER_STATUS_LABELS,
} from '../../../../models/order';
import { getErrorMessage } from '../../../../utils/http-error';

type OperatorAction = 'confirm' | 'cook' | 'send' | 'deliver';

const NEXT_ACTION: Partial<Record<OrderStatus, OperatorAction>> = {
  PENDING: 'confirm',
  CONFIRMED: 'cook',
  PREPARING: 'send',
  ON_THE_WAY: 'deliver',
};

const ACTION_LABELS: Record<OperatorAction, string> = {
  confirm: 'Confirmar',
  cook: 'Cocinar',
  send: 'Enviar',
  deliver: 'Completar',
};

const STATUS_COLORS: Record<OrderStatus, string> = {
  PENDING: '#e15e40',
  CONFIRMED: '#d99a21',
  PREPARING: '#b2571f',
  ON_THE_WAY: '#556141',
  DELIVERED: '#94c04f',
  CANCELLED: '#da5858',
};

@Component({
  selector: 'app-operator-orders',
  imports: [DatePipe, DecimalPipe, ButtonComponent, OrderProgressComponent],
  templateUrl: './operator-orders.component.html',
  styleUrl: './operator-orders.component.scss',
})
export class OperatorOrdersComponent {
  private orderService = inject(OrderService);

  @Input({ required: true }) operatorId!: number;
  @Output() sendRequested = new EventEmitter<OrderSummary>();
  @Output() detailRequested = new EventEmitter<number>();

  orders: OrderSummary[] = [];

  errorMessage = '';

  // Aviso temporal (toast) que se muestra al completar un pedido
  toastMessage = '';
  private toastTimer: ReturnType<typeof setTimeout> | null = null;

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

  onAction(orderSummary: OrderSummary) {
    const action = NEXT_ACTION[orderSummary.status];

    if (!action || this.busyOrderId !== null) {
      return;
    }

    if (action === 'send') {
      this.sendRequested.emit(orderSummary);
      return;
    }

    const request$ =
      action === 'confirm'
        ? this.orderService.confirmOrder(orderSummary.id)
        : action === 'cook'
          ? this.orderService.cookOrder(orderSummary.id, this.operatorId)
          : this.orderService.deliverOrder(orderSummary.id);

    this.errorMessage = '';
    this.busyOrderId = orderSummary.id;

    request$.pipe(finalize(() => (this.busyOrderId = null))).subscribe({
      next: () => {
        this.orderService.refresh();

        if (action === 'deliver') {
          this.showToast(`El pedido #${orderSummary.id} fue completado exitosamente.`);
        }
      },
      error: (error) => {
        console.error('Error updating order:', error);
        this.errorMessage =
          getErrorMessage(error, 'No se pudo actualizar el pedido.');
      },
    });
  }

  showToast(message: string) {
    if (this.toastTimer) {
      clearTimeout(this.toastTimer);
    }

    this.toastMessage = message;
    this.toastTimer = setTimeout(() => this.closeToast(), 4500);
  }

  closeToast() {
    if (this.toastTimer) {
      clearTimeout(this.toastTimer);
      this.toastTimer = null;
    }

    this.toastMessage = '';
  }

  actionLabel(orderSummary: OrderSummary): string | null {
    const action = NEXT_ACTION[orderSummary.status];
    return action ? ACTION_LABELS[action] : null;
  }

  clientName(orderSummary: OrderSummary): string {
    return orderSummary.clientName ?? 'Cliente eliminado';
  }

  deliveryPersonName(orderSummary: OrderSummary): string {
    return orderSummary.deliveryPersonName ?? '—';
  }
}