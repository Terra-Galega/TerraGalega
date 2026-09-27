import { Component, inject } from '@angular/core';
import { Product } from '../../models/product';
import { ProductService } from '../../services/product.service';
import {ProductCardMenuComponent} from './components/product-card-menu/product-card-menu.component';

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
  imports: [ProductCardMenuComponent],
  templateUrl: './menu.component.html',
  styleUrl: './menu.component.scss',
})
export class MenuComponent {
  private productService = inject(ProductService);

  products: Product[] = this.productService.getActiveProducts();

  filteredProducts: Product[] = [...this.products];

  searchTerm = '';

  selectedCategory = 'Todos';

  showFeatureFilters = false;

  categories = [
    'Todos',
    'Popular',
    'Entradas',
    'Mariscos',
    'Carnes',
    'Postres',
    'Bebidas',
  ];

  selectedFeatures: Record<FeatureKey, boolean> = {
    vegetarian: false,
    spicyMild: false,
    spicyHot: false,
    containsNuts: false,
    containsSeafood: false,
    containsGluten: false,
  };

  onSearch(event: Event): void {
    const input = event.target as HTMLInputElement;

    this.searchTerm = input.value;

    this.filterProducts();
  }

  selectCategory(category: string): void {
    this.selectedCategory = category;

    this.filterProducts();
  }

  toggleFeature(feature: FeatureKey): void {
    this.selectedFeatures[feature] =
      !this.selectedFeatures[feature];

    this.filterProducts();
  }

  filterProducts(): void {
    this.filteredProducts = this.products.filter((product) => {
      const matchesSearch =
        product.name
          .toLowerCase()
          .includes(this.searchTerm.toLowerCase()) ||
        product.description
          .toLowerCase()
          .includes(this.searchTerm.toLowerCase());

      const matchesCategory =
        this.selectedCategory === 'Todos' ||
        (this.selectedCategory === 'Popular' && product.popular) ||
        product.category?.name === this.selectedCategory;

      const matchesFeatures =
        (!this.selectedFeatures.vegetarian ||
          product.vegetarian) &&
        (!this.selectedFeatures.spicyMild ||
          product.spicyMild) &&
        (!this.selectedFeatures.spicyHot ||
          product.spicyHot) &&
        (!this.selectedFeatures.containsNuts ||
          product.containsNuts) &&
        (!this.selectedFeatures.containsSeafood ||
          product.containsSeafood) &&
        (!this.selectedFeatures.containsGluten ||
          product.containsGluten);

      return (
        matchesSearch &&
        matchesCategory &&
        matchesFeatures
      );
    });
  }

  resetFilters(): void {
    this.searchTerm = '';

    this.selectedCategory = 'Todos';

    this.selectedFeatures = {
      vegetarian: false,
      spicyMild: false,
      spicyHot: false,
      containsNuts: false,
      containsSeafood: false,
      containsGluten: false,
    };

    this.filteredProducts = [...this.products];
  }
}