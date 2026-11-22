import { TranslatePipe } from '../../pipes/translate.pipe';
import { Component, DestroyRef, inject } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { ReviewCardComponent } from '../review-card/review-card.component';
import { StarIconComponent } from '../star-icon/star-icon.component';
import { Review } from '../../models/review';
import { ReviewService } from '../../services/review.service';

// Con menos reseñas que esto se muestran quietas; con más, en carrusel
const MIN_FOR_CAROUSEL = 4;
// Tarjetas mínimas en cada mitad del carrusel, para que no queden huecos en pantallas anchas
const MIN_CARDS = 8;
// Segundos que tarda el carrusel en pasar cada tarjeta
const SECONDS_PER_CARD = 6;

/**
 * Reseñas de los clientes: las pide al back y las muestra en carrusel (o quietas
 * si son pocas). Mientras cargan muestra tarjetas de espera. Se recarga sola
 * cuando alguien guarda o borra una reseña. No incluye el encabezado: cada
 * página pone el suyo.
 */
@Component({
  selector: 'app-reviews-track',
  imports: [ReviewCardComponent, StarIconComponent, TranslatePipe],
  templateUrl: './reviews-track.component.html',
  styleUrl: './reviews-track.component.scss',
})
export class ReviewsTrackComponent {
  private destroyRef = inject(DestroyRef);

  // Tarjetas de espera mientras llegan las reseñas
  readonly placeholders = [1, 2, 3, 4, 5, 6];
  readonly placeholderLoop = [...this.placeholders, ...this.placeholders];

  // true hasta que el back responde por primera vez
  loading = true;
  // Reseñas visibles que llegaron del back
  reviews: Review[] = [];
  // Reseñas del carrusel: la lista repetida dos veces para que el desplazamiento sea continuo
  loop: Review[] = [];
  // Duración de una vuelta; crece con la cantidad de tarjetas para que la velocidad no cambie
  duration = '40s';

  constructor(private reviewService: ReviewService) {
    this.load();

    // Cuando se guarda o se borra una reseña, la lista se vuelve a pedir
    this.reviewService.refresh$
      .pipe(takeUntilDestroyed())
      .subscribe(() => this.load());
  }

  // true si hay suficientes reseñas para el carrusel animado
  get animated(): boolean {
    return this.reviews.length >= MIN_FOR_CAROUSEL;
  }

  // Pide las reseñas visibles al back
  private load() {
    this.reviewService
      .getReviews()
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe({
        next: (reviews) => {
          this.reviews = reviews;
          this.buildLoop();
          this.loading = false;
        },
        error: () => {
          // Si falla la primera carga se muestra "aún no hay opiniones";
          // si falla una recarga se conserva la lista que ya había
          if (this.loading) {
            this.reviews = [];
            this.loop = [];
          }
          this.loading = false;
        },
      });
  }

  // Repite las reseñas hasta llegar a MIN_CARDS y duplica la lista
  private buildLoop() {
    if (!this.animated) {
      this.loop = [];
      return;
    }
    const copies = Math.ceil(MIN_CARDS / this.reviews.length);
    const half = Array.from({ length: copies }, () => this.reviews).flat();
    this.loop = [...half, ...half];
    this.duration = `${half.length * SECONDS_PER_CARD}s`;
  }
}