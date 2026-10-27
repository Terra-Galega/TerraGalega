import { Component, EventEmitter, Input, Output } from '@angular/core';

// Selector de cantidad. El mínimo es 1.
// Se usa con enlace doble: <app-quantity-selector [(value)]="quantity" />
@Component({
  selector: 'app-quantity-selector',
  templateUrl: './quantity-selector.component.html',
  styleUrl: './quantity-selector.component.scss',
})
export class QuantitySelectorComponent {
  // Cantidad actual
  @Input() value = 1;
  // Valor mínimo permitido
  @Input() min = 1;

  // Se emite la nueva cantidad cuando cambia
  @Output() valueChange = new EventEmitter<number>();

  // Suma o resta (delta = +1 o -1); si el resultado queda por debajo del
  // mínimo, no se emite nada y la cantidad no cambia
  change(delta: number): void {
    const next = this.value + delta;
    if (next >= this.min) this.valueChange.emit(next);
  }
}
