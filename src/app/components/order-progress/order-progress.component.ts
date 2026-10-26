import { Component, Input } from '@angular/core';
import { OrderStatus, ORDER_STATUS_LABELS } from '../../models/order';

type StepState = 'done' | 'current' | 'upcoming';

// Flujo normal de un pedido (el cancelado no entra aquí)
const FLOW: OrderStatus[] = [
  'PENDING',
  'CONFIRMED',
  'PREPARING',
  'ON_THE_WAY',
  'DELIVERED',
];

// Solo presentación: recibe el estado y dibuja qué pasos ya se completaron
@Component({
  selector: 'app-order-progress',
  templateUrl: './order-progress.component.html',
})
export class OrderProgressComponent {
  @Input({ required: true }) status!: OrderStatus;

  get cancelled(): boolean {
    return this.status === 'CANCELLED';
  }

  get steps(): { label: string; state: StepState }[] {
    const currentIndex = FLOW.indexOf(this.status);

    return FLOW.map((step, index) => ({
      label: ORDER_STATUS_LABELS[step],
      state:
        index < currentIndex || this.status === 'DELIVERED'
          ? 'done'
          : index === currentIndex
            ? 'current'
            : 'upcoming',
    }));
  }
}