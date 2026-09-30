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
  host: { '[class.is-inactive]': '!product.active' },
})
export class AdminProductRowComponent {
  @Input({ required: true }) product!: Product;

  @Output() toggle = new EventEmitter<Product>();
  @Output() edit = new EventEmitter<Product>();
  @Output() remove = new EventEmitter<Product>();
}