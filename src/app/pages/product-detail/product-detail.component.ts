import { Component, inject, OnInit, DestroyRef } from '@angular/core';
import { ActivatedRoute, Router } from '@angular/router';
import { NavbarComponent } from '../../components/navbar/navbar.component';
import { FooterComponent } from '../../components/footer/footer.component';
import { Product, ProductDetail } from '../../models/product';
import { ProductService } from '../../services/product.service';
import { ProductInfoCardComponent } from './components/product-info-card/product-info-card.component';
import { ProductCardComponent } from '../../components/product-card/product-card.component';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import {
  EMPTY,
  Subscription,
  catchError,
  filter,
  forkJoin,
  fromEvent,
  merge,
  of,
  switchMap,
  timer,
} from 'rxjs';
import {
  getErrorMessage,
  isNetworkOrServerError,
} from '../../utils/http-error';

// Cada cuánto se vuelve a pedir el producto al servidor (en milisegundos)
const REFRESH_MS = 30_000;

/**
 * Página de detalle de un producto. Lee el id de la dirección (/productDetails/:id),
 * carga el producto y sus productos relacionados, y los mantiene actualizados.
 * Si el producto deja de existir o el admin lo desactiva, lleva a la página de error.
 */
@Component({
  selector: 'app-product-detail',
  standalone: true,
  imports: [
    NavbarComponent,
    ProductInfoCardComponent,
    ProductCardComponent,
    FooterComponent,
  ],
  templateUrl: './product-detail.component.html',
})
export class ProductDetailComponent implements OnInit {
  private route = inject(ActivatedRoute);
  private router = inject(Router);
  private productService = inject(ProductService);
  private destroyRef = inject(DestroyRef);
  // Suscripción de la actualización automática, para poder cancelarla
  private refreshSub?: Subscription;

  // Producto que se muestra (undefined mientras carga)
  product: ProductDetail | undefined;

  // "También te puede gustar": hasta 3 productos de la misma categoría
  relatedProducts: Product[] = [];

  ngOnInit(): void {
    // Cada vez que cambia el id de la dirección (por ejemplo, al pulsar un
    // producto relacionado) se carga el producto nuevo
    this.route.paramMap.subscribe((params) => {
      this.loadProduct(Number(params.get('id')));
    });
  }

  // Carga el producto y sus relacionados, y deja la actualización automática
  private loadProduct(id: number): void {
    // Se limpia lo que se veía del producto anterior
    this.product = undefined;
    this.relatedProducts = [];

    // La página vuelve arriba al cambiar de producto
    window.scrollTo({ top: 0 });

    // Al cambiar de producto se reinicia la actualización automática
    this.refreshSub?.unsubscribe();

    // Carga el producto al abrir, lo actualiza cada REFRESH_MS y también al
    // volver a la pestaña
    this.refreshSub = merge(
      timer(0, REFRESH_MS),
      fromEvent(document, 'visibilitychange'),
    )
      .pipe(
        // Solo se pide si la pestaña está visible
        filter(() => document.visibilityState === 'visible'),
        // switchMap cancela la petición anterior si llega una nueva antes de terminar
        switchMap(() =>
          // lanza las dos peticiones a la vez y espera a que las dos
          // terminen, entrega un solo objeto con los resultados
          forkJoin({
            // si esta petición falla, falla todo el forkJoin (sin producto
            // no hay nada que mostrar) y lo atrapa el catchError de más abajo
            product: this.productService.getProductById(id),
            // Si fallan los relacionados, se dejan los que ya se veían
            related: this.productService
              .getRelatedProducts(id)
              .pipe(catchError(() => of(null))),
          }).pipe(
            catchError((error) => {
              this.handleLoadError(error);
              return EMPTY;
            }),
          ),
        ),
        // Deja de actualizar cuando el componente se destruye (al salir de la página)
        takeUntilDestroyed(this.destroyRef),
      )
      .subscribe(({ product, related }) => {
        // El admin lo desactivó mientras se estaba viendo
        if (product.active === false) {
          this.showUnavailable();
          return;
        }

        this.product = product;

        // related es null si esa petición falló: en ese caso se conservan los anteriores
        if (related) {
          this.relatedProducts = related;
        }
      });
  }

  // Decide qué hacer cuando falla la carga del producto
  private handleLoadError(error: any): void {
    console.error('Error cargando producto:', error);

    // Si ya se estaba mostrando el producto y solo falló la conexión o el
    // servidor, se deja lo que se ve y se reintenta en el siguiente ciclo.
    if (this.product && isNetworkOrServerError(error)) {
      return;
    }

    // En cualquier otro caso (no existe, no está disponible) se va a la página de error
    this.showUnavailable(
      getErrorMessage(
        error,
        'Este producto no existe o ya no está disponible.',
      ),
    );
  }

  // Detiene la actualización automática y lleva a la página de error con un mensaje
  private showUnavailable(
    message = 'Este producto ya no está disponible.',
  ): void {
    this.refreshSub?.unsubscribe();

    // replaceUrl evita que "atrás" regrese a esta página rota
    this.router.navigate(['/error'], {
      replaceUrl: true,
      queryParams: { message },
    });
  }

  // Botón "Volver a la carta"
  goBackToMenu(): void {
    this.router.navigate(['/menu']);
  }
}
