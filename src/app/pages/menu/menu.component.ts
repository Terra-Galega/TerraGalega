import { Component, inject } from '@angular/core';
import { NavbarComponent } from '../../components/navbar/navbar.component';
import { FooterComponent } from '../../components/footer/footer.component';
import { PageHeaderComponent } from '../../components/page-header/page-header.component';
import { SearchInputComponent } from '../../components/search-input/search-input.component';
import { EmptyStateComponent } from '../../components/empty-state/empty-state.component';
import { CategoryFilterComponent } from './components/category-filter/category-filter.component';
import { FeatureFilterComponent } from './components/feature-filter/feature-filter.component';
import { ProductCardMenuComponent } from './components/product-card-menu/product-card-menu.component';
import { ProductService } from '../../services/product.service';
import { Product } from '../../models/product';
import { FeatureKey } from '../../models/feature';

@Component({
  selector: 'app-menu',
  imports: [
    NavbarComponent,
    FooterComponent,
    PageHeaderComponent,
    SearchInputComponent,
    EmptyStateComponent,
    CategoryFilterComponent,
    FeatureFilterComponent,
    ProductCardMenuComponent,
  ],
  templateUrl: './menu.component.html',
  styleUrl: './menu.component.scss',
})
export class MenuComponent {
  private productService = inject(ProductService);

  products: Product[] = this.productService.getActiveProducts();
  filteredProducts: Product[] = [...this.products];

  currentCategory = 'Todos';
  currentSearch = '';
  activeFeatureFilters: FeatureKey[] = [];

  readonly categories = ['Todos', 'Popular', 'Entradas', 'Mariscos', 'Carnes', 'Postres', 'Bebidas'];

  filterMenuItems(): void {
    const search = this.currentSearch.trim().toLowerCase();

    this.filteredProducts = this.products.filter((product) => {
      const matchesCategory =
        this.currentCategory === 'Todos' ||
        (this.currentCategory === 'Popular'
          ? product.popular
          : product.category?.name === this.currentCategory);

      const matchesSearch = product.name.toLowerCase().includes(search);

      // Con varias características, el plato debe cumplirlas todas
      const matchesFeatures = this.activeFeatureFilters.every((feature) => product[feature]);

      return matchesCategory && matchesSearch && matchesFeatures;
    });
  }

  selectCategory(category: string): void {
    this.currentCategory = category;
    this.filterMenuItems();
  }

  onSearch(term: string): void {
    this.currentSearch = term;
    this.filterMenuItems();
  }

  applyFeatureFilters(features: FeatureKey[]): void {
    this.activeFeatureFilters = features;
    this.filterMenuItems();
  }

  resetMenu(): void {
    this.currentSearch = '';
    this.currentCategory = 'Todos';
    this.activeFeatureFilters = [];
    this.filterMenuItems();
  }
}