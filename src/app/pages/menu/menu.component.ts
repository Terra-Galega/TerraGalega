import { Component, inject } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import {
  EMPTY,
  catchError,
  filter,
  fromEvent,
  merge,
  switchMap,
  timer,
} from 'rxjs';
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
import { CategoryService } from '../../services/category.service';
import { Category } from '../../models/category';
import { FeatureKey } from '../../models/feature';

// Cada cuánto se vuelve a pedir el menú al servidor (en milisegundos)
const REFRESH_MS = 10_000;

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
})
export class MenuComponent {
  private productService = inject(ProductService);
  private categoryService = inject(CategoryService);

  products: Product[] = [];
  filteredProducts: Product[] = [];
  categories: Category[] = [];

  constructor() {
    this.categoryService.getCategories().subscribe({
      next: (categories) => {
        this.categories = categories;
      },
      error: (error) => {
        console.error('Error cargando categorías:', error);
      },
    });

    // carga el menú al abrir, lo actualiza cada REFRESH_MS y también al volver
    // a esta pestaña
    // los cambios hechos en el admin aparecen sin recargar,
    // y el buscador, la categoría y los filtros se conservan.
    merge(timer(0, REFRESH_MS), fromEvent(document, 'visibilitychange'))
      .pipe(
        filter(() => document.visibilityState === 'visible'),
        switchMap(() =>
          this.productService.getActiveProducts().pipe(
            catchError((error) => {
              console.error('Error cargando productos:', error);
              return EMPTY; // si falla, se queda el menú que ya se veía
            }),
          ),
        ),
        takeUntilDestroyed(),
      )
      .subscribe((products) => {
        this.products = products;
        this.filterMenuItems();
      });
  }

  currentCategory = 'Todos';
  currentSearch = '';
  activeFeatureFilters: FeatureKey[] = [];

  get categoriesList(): string[] {
    return [
      'Todos',
      'Popular',
      ...this.categories.map((category) => category.name),
    ];
  }
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
      const matchesFeatures = this.activeFeatureFilters.every(
        (feature) => product[feature],
      );

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
