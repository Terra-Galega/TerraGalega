import { DatePipe, DecimalPipe } from '@angular/common';
import {
  Order,
  OrderDetail,
  ORDER_STATUS_LABELS,
  groupAddOns,
} from '../../../../models/order';

import { Component, EventEmitter, Input, Output, inject } from '@angular/core';
import { OrderService } from '../../../../services/order.service';
import { AuthService } from '../../../../services/auth.service';
import {
  ModalComponent,
  getErrorMessage,
} from '../../../../components/modal/modal.component';
import { ButtonComponent } from '../../../../components/button/button.component';

@Component({
  selector: 'app-ordered',
  imports: [DatePipe, DecimalPipe, ModalComponent, ButtonComponent],
  templateUrl: './ordered.component.html',
  styleUrl: './ordered.component.scss',
})
export class OrderedComponent {
  @Input() orders: Order[] = [];

  private orderService = inject(OrderService);
  private auth = inject(AuthService);

  // Avisa a la página que un pedido cambió para que recargue la lista
  @Output() orderCancelled = new EventEmitter<void>();

  showCancelModal = false;
  showErrorModal = false;
  selectedOrderId: number | null = null;
  errorMessage = '';

  askCancel(order: Order) {
    this.selectedOrderId = order.id;
    this.showCancelModal = true;
  }

  closeCancelModal() {
    this.showCancelModal = false;
    this.selectedOrderId = null;
  }

  confirmCancel() {
    const session = this.auth.session;

    if (
      this.selectedOrderId === null ||
      !session ||
      session.type !== 'client'
    ) {
      return;
    }

    const orderId = this.selectedOrderId;
    this.closeCancelModal();

    this.orderService.cancelOrder(orderId, session.id).subscribe({
      next: () => this.orderCancelled.emit(),
      error: (error) => {
        console.error('Error cancelando pedido:', error);
        this.errorMessage = getErrorMessage(
          error,
          'No se pudo cancelar el pedido. Inténtalo de nuevo.',
        );
        this.showErrorModal = true;
        // Se recarga por si el estado cambió (p. ej. ya lo confirmaron)
        this.orderCancelled.emit();
      },
    });
  }

  closeErrorModal() {
    this.showErrorModal = false;
    this.errorMessage = '';
  }

  statusLabels = ORDER_STATUS_LABELS;
  groupAddOns = groupAddOns;
  itemSubtotal(item: OrderDetail): number {
    const addOnsTotal = item.addOns.reduce(
      (sum, addOn) => sum + addOn.price,
      0,
    );
    return (item.unitPrice + addOnsTotal) * item.quantity;
  }
}
