import { Component, EventEmitter, Input, Output } from '@angular/core';
import { DecimalPipe } from '@angular/common';
import { AddOn } from '../../../../models/addOn';

// Fila de un adicional opcional: nombre + precio + selector de cantidad.
@Component({
  selector: 'app-addon-option',
  imports: [DecimalPipe],
  templateUrl: './addon-option.component.html',
  styleUrl: './addon-option.component.scss',
})
export class AddonOptionComponent {
  @Input({ required: true }) addOn!: AddOn;
  @Input() quantity = 0;

  @Output() changed = new EventEmitter<number>();
}
