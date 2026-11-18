import { TranslatePipe } from '../../../../services/translate.pipe';
import { Component, Input } from '@angular/core';
import { Product } from '../../../../models/product';
import { ProductCardComponent } from '../../../../components/product-card/product-card.component';
import { SectionHeadingComponent } from '../../../../components/section-heading/section-heading.component';
import { RevealDirective } from '../../../../directives/reveal.directive';

// Máximo de favoritos que se ven a la vez: dos filas de tres.
const MAX_STATIC = 6;
// Segundos que tarda cada tarjeta en recorrer su ancho en las filas animadas.
const SECONDS_PER_CARD = 7;

// Sección Favoritos de la casa.
// - Con 6 favoritos o menos se muestran fijos en una cuadrícula.
// - Con más de 6 se reparten en dos filas que se mueven en sentidos contrarios (la de arriba
//   hacia la derecha y la de abajo hacia la izquierda). En cada fila se ven 3 platos y los
//   bordes difuminan los platos que entran y salen.
@Component({
  selector: 'app-popular-products-section',
  imports: [ProductCardComponent, SectionHeadingComponent, RevealDirective, TranslatePipe],
  templateUrl: './popular-products-section.component.html',
  styleUrl: './popular-products-section.component.scss',
})
export class PopularProductsSectionComponent {
  // Lista de productos recibida desde el home; mientras no llegue la respuesta queda vacía.
  @Input() set products(list: Product[]) {
    this.all = list ?? [];
    // Los platos se alternan entre las dos filas para que ambas tengan variedad.
    this.topRow = this.all.filter((_, i) => i % 2 === 0);
    this.bottomRow = this.all.filter((_, i) => i % 2 === 1);
  }

  all: Product[] = [];
  topRow: Product[] = [];
  bottomRow: Product[] = [];

  get animated(): boolean {
    return this.all.length > MAX_STATIC;
  }

  /** Duración de una vuelta completa de la fila según cuántos platos tiene. */
  durationOf(row: Product[]): string {
    return `${row.length * SECONDS_PER_CARD}s`;
  }
}
