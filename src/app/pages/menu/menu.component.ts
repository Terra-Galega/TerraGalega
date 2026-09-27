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

  constructor() {
    this.productService.productsChanged.subscribe(() => {
      this.products = this.productService.getActiveProducts();
      this.filterMenuItems();
    });
  }

  /*
   * Estado equivalente a:
   * let currentCategory = "Todos";
   * let currentSearch = "";
   * let activeFeatureFilters = [];
   */
  currentCategory = 'Todos';
  currentSearch = '';

  activeFeatureFilters: FeatureKey[] = [];

  /*
   * Estado del panel de filtros
   */
  featureFilterOpen = false;

  /*
   * Checkboxes temporales del panel.
   *
   * Se mantienen separados de activeFeatureFilters porque
   * en develop los filtros solamente se aplican al pulsar
   * "Aplicar".
   */
  featureFilters: Record<FeatureKey, boolean> = {
    vegetarian: false,
    spicyMild: false,
    spicyHot: false,
    containsNuts: false,
    containsSeafood: false,
    containsGluten: false,
  };

  /*
   * Categorías exactamente como aparecen en develop.
   */
  readonly categories = [
    'Todos',
    'Popular',
    'Entradas',
    'Mariscos',
    'Carnes',
    'Postres',
    'Bebidas',
  ];

  /*
   * Características del panel.
   */
  readonly featureOptions: {
    key: FeatureKey;
    label: string;
  }[] = [
    {
      key: 'vegetarian',
      label: 'Vegetariano',
    },
    {
      key: 'spicyMild',
      label: 'Picante ligero',
    },
    {
      key: 'spicyHot',
      label: 'Picante intenso',
    },
    {
      key: 'containsNuts',
      label: 'Contiene nueces',
    },
    {
      key: 'containsSeafood',
      label: 'Mariscos',
    },
    {
      key: 'containsGluten',
      label: 'Contiene gluten',
    },
  ];

  /**
   * Equivalente a filterMenuItems() de script.js.
   */
  filterMenuItems(): void {
    const search = this.currentSearch.trim().toLowerCase();

    this.filteredProducts = this.products.filter((product) => {
      /*
       * Categoría
       */
      const matchesCategory =
        this.currentCategory === 'Todos' ||
        (this.currentCategory === 'Popular'
          ? product.popular
          : product.category?.name === this.currentCategory);

      /*
       * Búsqueda por nombre.
       *
       * Igual que develop:
       * item.dataset.name.toLowerCase().includes(currentSearch)
       */
      const matchesSearch = product.name.toLowerCase().includes(search);

      /*
       * Características.
       *
       * Igual que:
       * activeFeatureFilters.every(...)
       *
       * Esto significa que si seleccionas Vegetariano + Mariscos,
       * el plato debe cumplir AMBAS.
       */
      const matchesFeatures = this.activeFeatureFilters.every(
        (feature) => product[feature],
      );

      return matchesCategory && matchesSearch && matchesFeatures;
    });
  }

  /**
   * Cambio de categoría.
   */
  selectCategory(category: string): void {
    this.currentCategory = category;
    this.filterMenuItems();
  }

  /**
   * Búsqueda de platos.
   *
   * Equivalente al evento "input" de script.js.
   */
  onSearch(event: Event): void {
    const input = event.target as HTMLInputElement;

    this.currentSearch = input.value;
    this.filterMenuItems();
  }

  /**
   * Abre/cierra el panel de características.
   */
  toggleFeatureFilter(): void {
    this.featureFilterOpen = !this.featureFilterOpen;
  }

  /**
   * Cambia temporalmente un checkbox.
   */
  toggleFeature(feature: FeatureKey): void {
    this.featureFilters[feature] = !this.featureFilters[feature];
  }

  /**
   * Aplica los filtros seleccionados.
   *
   * Equivalente a:
   * featureFilterApplyBtn.addEventListener(...)
   */
  applyFeatureFilters(): void {
    this.activeFeatureFilters = (
      Object.keys(this.featureFilters) as FeatureKey[]
    ).filter((feature) => this.featureFilters[feature]);

    this.featureFilterOpen = false;

    this.filterMenuItems();
  }

  /**
   * Limpia únicamente las características.
   *
   * Equivalente al botón "Limpiar" del panel.
   */
  clearFeatureFilters(): void {
    this.activeFeatureFilters = [];

    this.featureFilters = {
      vegetarian: false,
      spicyMild: false,
      spicyHot: false,
      containsNuts: false,
      containsSeafood: false,
      containsGluten: false,
    };

    this.filterMenuItems();
  }

  /**
   * Indica si existe al menos un filtro de características.
   *
   * Sirve para activar visualmente el botón.
   */
  get hasActiveFeatureFilters(): boolean {
    return this.activeFeatureFilters.length > 0;
  }

  /**
   * Equivalente a "Ver todos los platos".
   *
   * Limpia búsqueda, categoría y características.
   */
  resetMenu(): void {
    this.currentSearch = '';
    this.currentCategory = 'Todos';

    this.clearFeatureFilters();
  }

  /**
   * Comprueba si una característica está seleccionada
   * en el panel.
   */
  isFeatureSelected(feature: FeatureKey): boolean {
    return this.featureFilters[feature];
  }
}
