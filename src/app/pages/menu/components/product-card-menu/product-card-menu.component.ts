import { Component, Input } from '@angular/core';
import { Product } from '../../../../models/product';

@Component({
  selector: 'app-product-card-menu',
  imports: [],
  templateUrl: './product-card-menu.component.html',
  styleUrl: './product-card-menu.component.scss'
})
export class ProductCardMenuComponent {
  @Input() product!: Product;
}
