import { Component, inject, OnInit, DestroyRef } from '@angular/core';
import { ActivatedRoute, Router } from '@angular/router';
import { NavbarComponent } from '../../components/navbar/navbar.component';
import { FooterComponent } from '../../components/footer/footer.component';
import { Product } from '../../models/product';
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
import { getErrorMessage, isNetworkOrServerError } from '../../utils/http-error';

// Cada cuánto se vuelve a pedir el producto al servidor (en milisegundos)
const REFRESH_MS = 30_000;

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
  private refreshSub?: Subscription;

  product: Product | undefined;

  relatedProducts: Product[] = [];

  ngOnInit(): void {
    this.route.paramMap.subscribe((params) => {
      this.loadProduct(Number(params.get('id')));
    });
  }

  private loadProduct(id: number): void {
    this.product = undefined;
    this.relatedProducts = [];

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
        filter(() => document.visibilityState === 'visible'),
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
        takeUntilDestroyed(this.destroyRef),
      )
      .subscribe(({ product, related }) => {
        // El admin lo desactivó mientras se estaba viendo
        if (product.active === false) {
          this.showUnavailable();
          return;
        }

        this.product = product;

        if (related) {
          this.relatedProducts = related;
        }
      });
  }

  private handleLoadError(error: any): void {
    console.error('Error cargando producto:', error);

    // Si ya se estaba mostrando el producto y solo falló la conexión o el
    // servidor, se deja lo que se ve y se reintenta en el siguiente ciclo.
    if (this.product && isNetworkOrServerError(error)) {
      return;
    }

    this.showUnavailable(
      getErrorMessage(error, 'Este producto no existe o ya no está disponible.'),
    );
  }

  private showUnavailable(
    message = 'Este producto ya no está disponible.',
  ): void {
    this.refreshSub?.unsubscribe();

    this.router.navigate(['/error'], {
      replaceUrl: true,
      queryParams: { message },
    });
  }

  goBackToMenu(): void {
    this.router.navigate(['/menu']);
  }
}