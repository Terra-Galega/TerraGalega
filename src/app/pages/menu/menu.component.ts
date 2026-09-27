import { Component, inject } from '@angular/core';
import { ProductCardMenuComponent } from './components/product-card-menu/product-card-menu.component';
import { NavbarComponent } from '../../components/navbar/navbar.component';
import { FooterComponent } from '../../components/footer/footer.component';
import { ProductService } from '../../services/product.service';

import { Product } from '../../models/product';
type FeatureKey =
  | 'vegetarian'
  | 'spicyMild'
  | 'spicyHot'
  | 'containsNuts'
  | 'containsSeafood'
  | 'containsGluten';
@Component({
  selector: 'app-menu',
  standalone: true,
  imports: [ProductCardMenuComponent, NavbarComponent, FooterComponent],
  templateUrl: './menu.component.html',
  styleUrl: './menu.component.scss',
})
export class MenuComponent {
  private productService = inject(ProductService);
  products: Product[] = this.productService.getActiveProducts();
  filteredProducts: Product[] = [...this.products];
}
