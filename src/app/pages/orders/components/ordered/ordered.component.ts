import { Component, Input } from '@angular/core';
import { DatePipe, DecimalPipe } from '@angular/common';
import {
  Order,
  OrderDetail,
  ORDER_STATUS_LABELS,
} from '../../../../models/order';
@Component({
  selector: 'app-ordered',
  imports: [DatePipe, DecimalPipe],
  templateUrl: './ordered.component.html',
  styleUrl: './ordered.component.scss',
})
export class OrderedComponent {
  @Input() orders: Order[] = [];

  statusLabels = ORDER_STATUS_LABELS;

  itemSubtotal(item: OrderDetail): number {
    const addOnsTotal = item.addOns.reduce(
      (sum, addOn) => sum + addOn.price,
      0,
    );
    return (item.unitPrice + addOnsTotal) * item.quantity;
  }
}