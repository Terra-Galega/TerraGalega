import { Component, EventEmitter, Input, Output } from '@angular/core';
import { DecimalPipe } from '@angular/common';
import { AddOnOption } from '../../../../models/addOn';

// Fila de un adicional opcional: nombre + precio + selector de cantidad.
// No guarda la cantidad: la recibe del padre y le avisa cuando el usuario
// pulsa "+" o "−".
@Component({
  selector: 'app-addon-option',
  imports: [DecimalPipe],
  templateUrl: './addon-option.component.html',
  styleUrl: './addon-option.component.scss',
})
export class AddonOptionComponent {
  // Adicional que muestra la fila (id, nombre y precio)
  @Input({ required: true }) addOn!: AddOnOption;
  // Cuántas unidades de este adicional hay elegidas
  @Input() quantity = 0;

  // Se emite +1 al pulsar "+" y -1 al pulsar "−"
  @Output() changed = new EventEmitter<number>();
}
