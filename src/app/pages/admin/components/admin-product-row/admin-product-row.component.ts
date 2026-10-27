import { Component, EventEmitter, Input, Output } from '@angular/core';
import { DecimalPipe } from '@angular/common';
import { Product } from '../../../../models/product';
import { IconButtonComponent } from '../../../../components/icon-button/icon-button.component';
import { StarIconComponent } from '../../../../components/star-icon/star-icon.component';

// Fila de la tabla de productos. Se usa como atributo para conservar
// el HTML válido de la tabla usandose como <tr app-admin-product-row [product]="p" />
@Component({
  selector: 'tr[app-admin-product-row]',
  imports: [DecimalPipe, IconButtonComponent, StarIconComponent],
  templateUrl: './admin-product-row.component.html',
  styleUrl: './admin-product-row.component.scss',
})
export class AdminProductRowComponent {
  // Producto que muestra la fila
  @Input({ required: true }) product!: Product;

  // La fila no decide nada: solo avisa al padre qué botón se pulsó
  @Output() toggle = new EventEmitter<Product>(); // activar / desactivar
  @Output() edit = new EventEmitter<Product>(); // editar
  @Output() remove = new EventEmitter<Product>(); // eliminar
}
