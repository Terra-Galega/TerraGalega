import { Component, Input, inject } from '@angular/core';
import { Router } from '@angular/router';
import { DecimalPipe } from '@angular/common';
import { FeatureChipsComponent } from '../../../../components/feature-chips/feature-chips.component';
import { StarIconComponent } from '../../../../components/star-icon/star-icon.component';
import { Product } from '../../../../models/product';

@Component({
  selector: 'app-product-card-menu',
  standalone: true,
  imports: [DecimalPipe, FeatureChipsComponent, StarIconComponent],
  templateUrl: './product-card-menu.component.html',
  styleUrl: './product-card-menu.component.scss',
})
export class ProductCardMenuComponent {
  private router = inject(Router);

  @Input() product!: Product;

  openProductDetail(): void {
    this.router.navigate(['/productDetails', this.product.id]);
  }
}