import { Component } from '@angular/core';
import { SectionHeadingComponent } from '../../../../components/section-heading/section-heading.component';
import { StarIconComponent } from '../../../../components/star-icon/star-icon.component';

// Carrusel de opiniones de la página Nosotros. Las opiniones todavía no se
// cargan, así que se muestran tarjetas de espera que se desplazan solas.
@Component({
  selector: 'app-reviews-carousel',
  imports: [SectionHeadingComponent, StarIconComponent],
  templateUrl: './reviews-carousel.component.html',
  styleUrl: './reviews-carousel.component.scss',
})
export class ReviewsCarouselComponent {
  // Número de tarjetas de espera
  readonly cards = [1, 2, 3, 4, 5, 6];
  // La lista se repite dos veces para que el desplazamiento sea continuo
  readonly loop = [...this.cards, ...this.cards];
}
