import { TranslatePipe } from '../../../../services/translate.pipe';
import { Component, Input, inject } from '@angular/core';
import { Router, RouterLink } from '@angular/router';
import { DecimalPipe } from '@angular/common';
import { FeatureChipsComponent } from '../../../../components/feature-chips/feature-chips.component';
import { StarIconComponent } from '../../../../components/star-icon/star-icon.component';
import { Product } from '../../../../models/product';

// Tarjeta de un producto en la carta: imagen, insignias, nombre, descripción,
// características y precio. Al hacer clic lleva al detalle del producto.
// (Es distinta de app-product-card, que se usa en el inicio.)
@Component({
  selector: 'app-product-card-menu',
  standalone: true,
  imports: [DecimalPipe, FeatureChipsComponent, StarIconComponent, RouterLink, TranslatePipe],
  templateUrl: './product-card-menu.component.html',
  styleUrl: './product-card-menu.component.scss',
})
export class ProductCardMenuComponent {
  private router = inject(Router);

  // Producto que muestra la tarjeta (obligatorio, lo pasa la carta)
  @Input() product!: Product;

  // Lleva a la página de detalle del producto
  openProductDetail(): void {
    this.router.navigate(['/productDetails', this.product.id]);
  }
}
