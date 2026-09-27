import { Component, Input, inject } from '@angular/core';
import { Router } from '@angular/router';
import { Product } from '../../models/product';

@Component({
  selector: 'app-product-card',
  imports: [],
  templateUrl: './product-card.component.html',
  styleUrl: './product-card.component.scss'
})
export class ProductCardComponent {
  @Input() product!: Product;
  router = inject(Router);

  goToProductDetail(): void {
    this.router.navigate(['productDetails', this.product.id]);
  }
}
