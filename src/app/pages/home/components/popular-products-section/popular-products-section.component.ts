import { Component, Input } from '@angular/core';
import { Product } from '../../../../models/product';
import { ProductCardComponent } from '../../../../components/product-card/product-card.component';
import { SectionHeadingComponent } from '../../../../components/section-heading/section-heading.component';

// Sección Favoritos de la casa: muestra una tarjeta por cada producto recibido.
@Component({
  selector: 'app-popular-products-section',
  // ProductCardComponent dibuja cada producto y SectionHeadingComponent el encabezado de la sección.
  imports: [ProductCardComponent, SectionHeadingComponent],
  templateUrl: './popular-products-section.component.html',
  styleUrl: './popular-products-section.component.scss',
})
export class PopularProductsSectionComponent {
  // Lista de productos recibida desde el home; mientras no llegue la respuesta queda vacía y no se muestra ninguna tarjeta.
  @Input() products: Product[] = [];
}
