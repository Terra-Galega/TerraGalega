import { Component, Input } from '@angular/core';
import { OrderStatus, ORDER_STATUS_LABELS } from '../../models/order';

// Estado visual de cada paso de la barra de progreso
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
  // Estado actual del pedido, obligatorio para poder dibujar la barra
  @Input({ required: true }) status!: OrderStatus;

  // Un pedido cancelado no muestra pasos, solo una etiqueta
  get cancelled(): boolean {
    return this.status === 'CANCELLED';
  }

  // Lista de pasos con su nombre en español y su estado visual
  get steps(): { label: string; state: StepState }[] {
    // Posición del estado actual dentro del flujo
    const currentIndex = FLOW.indexOf(this.status);

    return FLOW.map((step, index) => ({
      label: ORDER_STATUS_LABELS[step],
      // Antes del actual = completado. Si el pedido ya se entregó,
      // todos los pasos (incluido el último) cuentan como completados.
      state:
        index < currentIndex || this.status === 'DELIVERED'
          ? 'done'
          : index === currentIndex
            ? 'current'
            : 'upcoming',
    }));
  }
}