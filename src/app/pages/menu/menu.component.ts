import { Component, inject } from '@angular/core';
import { MenuCardComponent } from '../../components/menu-card/menu-card.component';
import { Product } from '../../models/product.model';
import { ProductService } from '../../services/product.service';
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
  imports: [MenuCardComponent],
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
    this.searchTerm = input.value.trim();
    this.filterProducts();
  }
  selectCategory(category: string): void {
    this.selectedCategory = category;
    this.filterProducts();
  }
  toggleFeature(feature: FeatureKey): void {
    this.selectedFeatures[feature] = !this.selectedFeatures[feature];
    this.filterProducts();
  }
  clearFeatureFilters(): void {
    this.selectedFeatures = {
      vegetarian: false,
      spicyMild: false,
      spicyHot: false,
      containsNuts: false,
      containsSeafood: false,
      containsGluten: false,
    };
    this.filterProducts();
  }
  filterProducts(): void {
    this.filteredProducts = this.products.filter((product) => {
      /* * BÚSQUEDA * * Igual que en develop: * busca por nombre. */ const matchesSearch =
        product.name.toLowerCase().includes(this.searchTerm.toLowerCase());
      /* * CATEGORÍA */ const matchesCategory =
        this.selectedCategory === 'Todos' ||
        (this.selectedCategory === 'Popular'
          ? product.popular
          : product.category.name === this.selectedCategory);
      /* * CARACTERÍSTICAS * * every() significa que TODAS las características * seleccionadas tienen que cumplirse. * * Ejemplo: * vegetariano + gluten * -> debe ser vegetariano Y contener gluten. */ const activeFeatures =
        (Object.keys(this.selectedFeatures) as FeatureKey[]).filter(
          (feature) => this.selectedFeatures[feature],
        );
      const matchesFeatures = activeFeatures.every(
        (feature) => product[feature],
      );
      return matchesSearch && matchesCategory && matchesFeatures;
    });
  }
  resetFilters(): void {
    this.searchTerm = '';
    this.selectedCategory = 'Todos';
    this.clearFeatureFilters();
    this.showFeatureFilters = false;
  }
  hasActiveFeatureFilters(): boolean {
    return Object.values(this.selectedFeatures).some((value) => value);
  }
}
