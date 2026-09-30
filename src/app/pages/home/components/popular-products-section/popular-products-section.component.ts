import { Component, Input } from '@angular/core';
import { Product } from '../../../../models/product';
import { ProductCardComponent } from '../../../../components/product-card/product-card.component';
import { SectionHeadingComponent } from '../../../../components/section-heading/section-heading.component';

@Component({
  selector: 'app-popular-products-section',
  imports: [ProductCardComponent, SectionHeadingComponent],
  templateUrl: './popular-products-section.component.html',
  styleUrl: './popular-products-section.component.scss',
})
export class PopularProductsSectionComponent {
  @Input() products: Product[] = [];
}