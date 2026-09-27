import { Component, Input } from '@angular/core';
import { DecimalPipe } from '@angular/common';
import { Product } from '../../../../models/product';

@Component({
  selector: 'app-product-card-menu',
  standalone: true,
  imports: [DecimalPipe],
  templateUrl: './product-card-menu.component.html',
  styleUrl: './product-card-menu.component.scss',
})
export class ProductCardMenuComponent {
  @Input() product!: Product;
}