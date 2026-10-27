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

/**
 * Página de la carta: muestra los productos activos y permite filtrarlos por
 * texto, por categoría (incluida "Popular") y por características
 * (vegetariano, picante, alérgenos). Los tres filtros se combinan.
 */
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

  // Todos los productos activos que llegaron del servidor
  products: Product[] = [];
  // Los productos que se ven en pantalla, después de aplicar los filtros
  filteredProducts: Product[] = [];
  // Categorías que vienen del servidor, para armar los botones de filtro
  categories: Category[] = [];

  constructor() {
    // Las categorías se piden una sola vez al abrir la página
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
        // Solo se pide el menú si la pestaña está visible (no gasta peticiones en segundo plano)
        filter(() => document.visibilityState === 'visible'),
        // switchMap cancela la petición anterior si llega una nueva antes de terminar
        switchMap(() =>
          this.productService.getActiveProducts().pipe(
            catchError((error) => {
              console.error('Error cargando productos:', error);
              return EMPTY; // si falla, se queda el menú que ya se veía
            }),
          ),
        ),
        // Deja de actualizar cuando el componente se destruye (al salir de la página)
        takeUntilDestroyed(),
      )
      .subscribe((products) => {
        this.products = products;
        // Se vuelven a aplicar los filtros sobre la lista recién cargada
        this.filterMenuItems();
      });
  }

  // Estado actual de los filtros
  currentCategory = 'Todos';
  currentSearch = '';
  activeFeatureFilters: FeatureKey[] = [];

  // Nombres de los botones de categoría: "Todos" y "Popular" son fijos y
  // después van las categorías reales
  get categoriesList(): string[] {
    return [
      'Todos',
      'Popular',
      ...this.categories.map((category) => category.name),
    ];
  }

  // Aplica los tres filtros a la lista de productos. Un producto se muestra
  // solo si cumple categoría, búsqueda y características a la vez.
  filterMenuItems(): void {
    const search = this.currentSearch.trim().toLowerCase();

    this.filteredProducts = this.products.filter((product) => {
      // "Todos" no filtra; "Popular" usa la marca popular del producto;
      // cualquier otra opción compara con el nombre de su categoría
      const matchesCategory =
        this.currentCategory === 'Todos' ||
        (this.currentCategory === 'Popular'
          ? product.popular
          : product.category?.name === this.currentCategory);

      // Búsqueda por nombre, sin importar mayúsculas
      const matchesSearch = product.name.toLowerCase().includes(search);

      // Con varias características, el plato debe cumplirlas todas
      const matchesFeatures = this.activeFeatureFilters.every(
        (feature) => product[feature],
      );

      return matchesCategory && matchesSearch && matchesFeatures;
    });
  }

  // El usuario eligió otro botón de categoría
  selectCategory(category: string): void {
    this.currentCategory = category;
    this.filterMenuItems();
  }

  // El usuario escribió en el buscador
  onSearch(term: string): void {
    this.currentSearch = term;
    this.filterMenuItems();
  }

  // El usuario aplicó (o limpió) el panel de características
  applyFeatureFilters(features: FeatureKey[]): void {
    this.activeFeatureFilters = features;
    this.filterMenuItems();
  }

  // Quita todos los filtros y vuelve a mostrar toda la carta
  // (lo usa el botón "Ver todos los platos" cuando no hay resultados)
  resetMenu(): void {
    this.currentSearch = '';
    this.currentCategory = 'Todos';
    this.activeFeatureFilters = [];
    this.filterMenuItems();
  }
}
