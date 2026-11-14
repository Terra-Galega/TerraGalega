import { Component } from '@angular/core';
import { SectionHeadingComponent } from '../../../../components/section-heading/section-heading.component';
import { ReviewsTrackComponent } from '../../../../components/reviews-track/reviews-track.component';
import { ReviewCtaComponent } from '../../../../components/review-cta/review-cta.component';

// Sección de opiniones del home: encabezado, carrusel de reseñas reales y el
// botón para escribir (o editar) la propia.
@Component({
  selector: 'app-testimonials-section',
  imports: [SectionHeadingComponent, ReviewsTrackComponent, ReviewCtaComponent],
  templateUrl: './testimonials-section.component.html',
  styleUrl: './testimonials-section.component.scss',
})
export class TestimonialsSectionComponent {}