import {
  Component,
  EventEmitter,
  Input,
  Output,
  inject,
  DestroyRef,
} from '@angular/core';
import { DatePipe, DecimalPipe } from '@angular/common';
import {
  EMPTY,
  catchError,
  exhaustMap,
  filter,
  finalize,
  merge,
  timer,
} from 'rxjs';
import { OrderService } from '../../../../services/order.service';
import { ButtonComponent } from '../../../../components/button/button.component';
import { OrderProgressComponent } from '../../../../components/order-progress/order-progress.component';
import { OrderStatus, OrderSummary } from '../../../../models/order';
import { getErrorMessage } from '../../../../utils/http-error';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';

// Define las acciones permitidas para avanzar un pedido según su estado actual.
type OperatorAction = 'confirm' | 'cook' | 'send' | 'deliver';

// Siguiente acción según el estado del pedido; los estados finales no tienen ninguna.
function nextAction(status: OrderStatus): OperatorAction | null {
  switch (status) {
    case 'PENDING':
      return 'confirm';
    case 'CONFIRMED':
      return 'cook';
    case 'PREPARING':
      return 'send';
    case 'ON_THE_WAY':
      return 'deliver';
    case 'DELIVERED':
    case 'CANCELLED':
      return null;
  }
}

// Texto del botón de cada acción, para mantener coherencia entre los estados.
function actionText(action: OperatorAction): string {
  switch (action) {
    case 'confirm':
      return 'Confirmar';
    case 'cook':
      return 'Cocinar';
    case 'send':
      return 'Enviar';
    case 'deliver':
      return 'Completar';
  }
}

@Component({
  selector: 'app-operator-orders',
  imports: [DatePipe, DecimalPipe, ButtonComponent, OrderProgressComponent],
  templateUrl: './operator-orders.component.html',
  styleUrl: './operator-orders.component.scss',
})
export class OperatorOrdersComponent {
  private orderService = inject(OrderService);
  private destroyRef = inject(DestroyRef);

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

  constructor() {
    merge(timer(0, 3000), this.orderService.refresh$)
      .pipe(
        filter(() => document.visibilityState === 'visible'),

        exhaustMap(() =>
          this.orderService.getActiveOrders().pipe(
            catchError((error) => {
              this.errorMessage = getErrorMessage(
                error,
                'No se pudieron cargar los pedidos.',
              );

              return EMPTY;
            }),
          ),
        ),

        takeUntilDestroyed(this.destroyRef),
      )
      .subscribe((orders) => {
        this.orders = orders;
        this.errorMessage = '';
      });

    this.destroyRef.onDestroy(() => {
      if (this.toastTimer) {
        clearTimeout(this.toastTimer);
      }
    });
  }

  onAction(orderSummary: OrderSummary) {
    // Determina qué operación corresponde al estado actual del pedido.
    // en onAction():
    const action = nextAction(orderSummary.status);

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
          ? this.orderService.cookOrder(orderSummary.id)
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
    const action = nextAction(orderSummary.status);
    return action ? actionText(action) : null;
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
