import { DatePipe, DecimalPipe } from '@angular/common';
import { Component, EventEmitter, Input, Output, inject } from '@angular/core';
import {
  Order,
  OrderDetail,
  ORDER_STATUS_LABELS,
  groupAddOns,
} from '../../../../models/order';

import { OrderService } from '../../../../services/order.service';
import { AuthService } from '../../../../services/auth.service';
import { ModalComponent } from '../../../../components/modal/modal.component';
import { ButtonComponent } from '../../../../components/button/button.component';
import { OrderProgressComponent } from '../../../../components/order-progress/order-progress.component';
import { getErrorMessage } from '../../../../utils/http-error';

// Lista de tarjetas de pedidos (la reutilizan "activos" y "pasados").
// Cada tarjeta muestra progreso, productos, dirección y, si está pendiente, el botón de cancelar.
@Component({
  selector: 'app-ordered',
  imports: [
    DatePipe,
    DecimalPipe,
    ModalComponent,
    ButtonComponent,
    OrderProgressComponent,
  ],
  templateUrl: './ordered.component.html',
})
export class OrderedComponent {
  // Pedidos a mostrar, enviados por la página
  @Input() orders: Order[] = [];

  private orderService = inject(OrderService);
  private auth = inject(AuthService);

  // Avisa a la página que un pedido cambió para que recargue la lista
  @Output() orderCancelled = new EventEmitter<void>();

  // Estado de los modales de confirmación y error, y pedido seleccionado para cancelar
  showCancelModal = false;
  showErrorModal = false;
  selectedOrderId: number | null = null;
  errorMessage = '';

  // Paso 1 de cancelar: guarda el pedido y abre la confirmación
  askCancel(order: Order) {
    this.selectedOrderId = order.id;
    this.showCancelModal = true;
  }

  closeCancelModal() {
    this.showCancelModal = false;
    this.selectedOrderId = null;
  }

  // Paso 2: el cliente confirmó. Necesita pedido seleccionado y sesión de cliente.
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

  // Etiquetas en español de los estados, y agrupador de adicionales repetidos
  statusLabels = ORDER_STATUS_LABELS;
  groupAddOns = groupAddOns;
  // Subtotal de una línea = (precio unitario + suma de adicionales) × cantidad
  itemSubtotal(item: OrderDetail): number {
    const addOnsTotal = item.addOns.reduce(
      (sum, addOn) => sum + addOn.price,
      0,
    );
    return (item.unitPrice + addOnsTotal) * item.quantity;
  }
}