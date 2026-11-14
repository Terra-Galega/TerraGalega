import { Component, Input } from '@angular/core';
import { StarIconComponent } from '../star-icon/star-icon.component';
import { Review } from '../../models/review';

// Tarjeta de una reseña: estrellas, comentario, autor y mes. La usan el home y
// la página Nosotros, así las dos se ven igual.
@Component({
  selector: 'app-review-card',
  imports: [StarIconComponent],
  templateUrl: './review-card.component.html',
  styleUrl: './review-card.component.scss',
})
export class ReviewCardComponent {
  // Reseña que se muestra
  @Input({ required: true }) review!: Review;

  // Las cinco posiciones de estrella; las que superan la calificación se atenúan
  readonly stars = [1, 2, 3, 4, 5];

  // Mes y año de la reseña, por ejemplo "noviembre de 2026"
  get month(): string {
    const date = new Date(this.review.createdAt);
    return isNaN(date.getTime())
      ? ''
      : date.toLocaleDateString('es-CO', { month: 'long', year: 'numeric' });
  }
}
