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

// Define las acciones permitidas para avanzar un pedido según su estado actual.
type OperatorAction = 'confirm' | 'cook' | 'send' | 'deliver';

// Relaciona cada estado con su siguiente acción; los estados finales no tienen acción disponible.
const NEXT_ACTION: Record<OrderStatus, OperatorAction | null> = {
  PENDING: 'confirm',
  CONFIRMED: 'cook',
  PREPARING: 'send',
  ON_THE_WAY: 'deliver',
  DELIVERED: null,
  CANCELLED: null,
};

// Centraliza los textos de los botones para mantener coherencia entre los distintos estados.
const ACTION_LABELS: Record<OperatorAction, string> = {
  confirm: 'Confirmar',
  cook: 'Cocinar',
  send: 'Enviar',
  deliver: 'Completar',
};

// Asigna un color específico a cada estado para identificar visualmente el progreso del pedido.
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

  // Aviso temporal que se muestra al completar un pedido.
  toastMessage = '';
  private toastTimer: ReturnType<typeof setTimeout> | null = null;

  // Identifica el pedido cuya operación está en curso para impedir solicitudes simultáneas.
  busyOrderId: number | null = null;

  statusLabels = ORDER_STATUS_LABELS;
  statusColors = STATUS_COLORS;

  constructor() {
    this.loadOrders();

    // Recarga la lista cuando otro componente solicita actualizar los pedidos.
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
    // Determina qué operación corresponde al estado actual del pedido.
    const action = NEXT_ACTION[orderSummary.status];

    // Evita ejecutar acciones no permitidas o iniciar otra operación mientras hay una en curso.
    if (!action || this.busyOrderId !== null) {
      return;
    }

    if (action === 'send') {
      // Delegar el envío permite que otro componente gestione la asignación del repartidor.
      this.sendRequested.emit(orderSummary);
      return;
    }

    // Selecciona la petición correspondiente sin duplicar la lógica de suscripción.
    const request$ =
      action === 'confirm'
        ? this.orderService.confirmOrder(orderSummary.id)
        : action === 'cook'
          ? this.orderService.cookOrder(orderSummary.id, this.operatorId)
          : this.orderService.deliverOrder(orderSummary.id);

    this.errorMessage = '';
    this.busyOrderId = orderSummary.id;

    // Libera el bloqueo tanto si la petición termina correctamente como si falla.
    request$.pipe(finalize(() => (this.busyOrderId = null))).subscribe({
      next: () => {
        // Notifica a los componentes suscritos para que sincronicen la lista con el nuevo estado.
        this.orderService.refresh();

        if (action === 'deliver') {
          // Muestra una confirmación únicamente cuando el pedido se marca como entregado.
          this.showToast(
            `El pedido #${orderSummary.id} fue completado exitosamente.`,
          );
        }
      },
      error: (error) => {
        console.error('Error updating order:', error);
        // Prioriza el mensaje específico del servidor y utiliza uno genérico como alternativa.
        this.errorMessage = getErrorMessage(
          error,
          'No se pudo actualizar el pedido.',
        );
      },
    });
  }

  showToast(message: string) {
    // Cancela el cierre anterior para que un nuevo aviso permanezca visible durante todo su intervalo.
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
    // Devuelve null cuando el estado no admite más acciones, permitiendo ocultar el botón.
    const action = NEXT_ACTION[orderSummary.status];
    return action ? ACTION_LABELS[action] : null;
  }

  clientName(orderSummary: OrderSummary): string {
    // Evita mostrar un valor vacío cuando el cliente asociado ya no existe.
    return orderSummary.clientName ?? 'Cliente eliminado';
  }

  deliveryPersonName(orderSummary: OrderSummary): string {
    // Usa un marcador cuando todavía no hay un repartidor asignado.
    return orderSummary.deliveryPersonName ?? '—';
  }
}
