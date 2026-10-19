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
  ORDER_STATUS_LABELS,
} from '../../../../models/order';
import { OrderService } from '../../../../services/order.service';
import { IconButtonComponent } from '../../../../components/icon-button/icon-button.component';

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

  addOnNames(detail: OrderDetail): string {
    return detail.addOns.map((addOn) => addOn.name).join(', ');
  }

  lineTotal(detail: OrderDetail): number {
    const addOnsPrice = detail.addOns.reduce((sum, a) => sum + a.price, 0);
    return (detail.unitPrice + addOnsPrice) * detail.quantity;
  }

  close() {
    this.closed.emit();
  }
}