import { Component, EventEmitter, Input, Output } from '@angular/core';

// Selector de cantidad. El mínimo es 1.
@Component({
  selector: 'app-quantity-selector',
  templateUrl: './quantity-selector.component.html',
  styleUrl: './quantity-selector.component.scss',
})
export class QuantitySelectorComponent {
  @Input() value = 1;
  @Input() min = 1;

  @Output() valueChange = new EventEmitter<number>();

  change(delta: number): void {
    const next = this.value + delta;
    if (next >= this.min) this.valueChange.emit(next);
  }
}