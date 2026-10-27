import { Component, Input } from '@angular/core';
import { CommonModule } from '@angular/common';
import { groupAddOns } from '../../../../models/order';
// Resumen del pedido (productos, subtotal, domicilio y total). Lo comparten el paso 1 y el paso 2.
@Component({
  selector: 'app-order-summary',
  standalone: true,
  imports: [CommonModule],
  templateUrl: './order-summary.component.html',
})
export class OrderSummaryComponent {
  // Productos del carrito a mostrar
  @Input() items: any[] = [];
  // Subtotal de los productos (sin domicilio)
  @Input() total = 0;
  // Costo del domicilio; si es 0 (paso 1) la línea no se muestra
  @Input() deliveryFee = 0;
  // Función del padre que calcula el precio unitario de un ítem (producto + adicionales)
  @Input() unitPrice: (item: any) => number = () => 0;
  // Se expone al HTML para agrupar adicionales repetidos (queso, queso → x2 queso)
  groupAddOns = groupAddOns;
}
