import { Component, EventEmitter, Input, Output } from '@angular/core';
import { DecimalPipe } from '@angular/common';
import { AddOn } from '../../../../models/addOn';

// Fila de un adicional opcional: casilla + nombre + precio.
@Component({
  selector: 'app-addon-option',
  imports: [DecimalPipe],
  templateUrl: './addon-option.component.html',
  styleUrl: './addon-option.component.scss',
})
export class AddonOptionComponent {
  @Input({ required: true }) addOn!: AddOn;
  @Input() selected = false;

  @Output() toggled = new EventEmitter<AddOn>();
}