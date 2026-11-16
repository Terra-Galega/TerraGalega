import { TranslatePipe } from '../../../../services/translate.pipe';
import { Component } from '@angular/core';
import { SectionHeadingComponent } from '../../../../components/section-heading/section-heading.component';
import { ReviewsTrackComponent } from '../../../../components/reviews-track/reviews-track.component';
import { ReviewCtaComponent } from '../../../../components/review-cta/review-cta.component';

// Opiniones de la página Nosotros: encabezado, carrusel de reseñas reales y el
// botón para escribir (o editar) la propia.
@Component({
  selector: 'app-reviews-carousel',
  imports: [SectionHeadingComponent, ReviewsTrackComponent, ReviewCtaComponent, TranslatePipe],
  templateUrl: './reviews-carousel.component.html',
  styleUrl: './reviews-carousel.component.scss',
})
export class ReviewsCarouselComponent {}