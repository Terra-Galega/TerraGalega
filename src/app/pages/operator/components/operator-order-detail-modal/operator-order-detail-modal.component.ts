import {
  Component,
  EventEmitter,
  Input,
  OnInit,
  Output,
  inject,
} from '@angular/core';
import { DatePipe, DecimalPipe } from '@angular/common';
import {
  Order,
  OrderDetail,
  OrderStatus,
  ORDER_STATUS_LABELS,
  groupAddOns,
} from '../../../../models/order';
import { OrderService } from '../../../../services/order.service';
import { IconButtonComponent } from '../../../../components/icon-button/icon-button.component';

const STATUS_COLORS: Record<OrderStatus, string> = {
  PENDING: '#d99a21',
  CONFIRMED: '#d99a21',
  PREPARING: '#b2571f',
  ON_THE_WAY: '#556141',
  DELIVERED: '#556141',
  CANCELLED: '#da5858',
};

@Component({
  selector: 'app-operator-order-detail-modal',
  imports: [DatePipe, DecimalPipe, IconButtonComponent],
  templateUrl: './operator-order-detail-modal.component.html',
})
export class OperatorOrderDetailModalComponent implements OnInit {
  private orderService = inject(OrderService);

  @Input({ required: true }) orderId!: number;

  @Output() closed = new EventEmitter<void>();

  order: Order | null = null;
  loading = true;
  errorMessage = '';

  statusLabels = ORDER_STATUS_LABELS;
  statusColors = STATUS_COLORS;
  groupAddOns = groupAddOns;
  // Pasos del flujo normal de un pedido (el cancelado no entra aquí)
  steps: OrderStatus[] = [
    'PENDING',
    'CONFIRMED',
    'PREPARING',
    'ON_THE_WAY',
    'DELIVERED',
  ];

  ngOnInit() {
    this.orderService.getOrderById(this.orderId).subscribe({
      next: (order) => {
        this.order = order;
        this.loading = false;
      },
      error: (error) => {
        console.error('Error loading order:', error);
        this.errorMessage = 'No se pudo cargar el pedido.';
        this.loading = false;
      },
    });
  }

  get stepIndex(): number {
    return this.order ? this.steps.indexOf(this.order.status) : -1;
  }

  lineTotal(detail: OrderDetail): number {
    const addOnsPrice = detail.addOns.reduce((sum, a) => sum + a.price, 0);
    return (detail.unitPrice + addOnsPrice) * detail.quantity;
  }

  // Si la imagen no carga, se oculta en vez de mostrar el ícono roto
  hideImage(event: Event) {
    (event.target as HTMLImageElement).style.display = 'none';
  }

  close() {
    this.closed.emit();
  }
}
