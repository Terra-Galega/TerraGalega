import { Component, inject } from '@angular/core';
import { FooterComponent } from '../../components/footer/footer.component';
import { NavbarComponent } from '../../components/navbar/navbar.component';
import { ProductService } from '../../services/product.service';
import { Product } from '../../models/product';
import { HeroSectionComponent } from './components/hero-section/hero-section.component';
import { PopularProductsSectionComponent } from './components/popular-products-section/popular-products-section.component';
import { StorySectionComponent } from './components/story-section/story-section.component';
import { TestimonialsSectionComponent } from './components/testimonials-section/testimonials-section.component';
import { ContactStripSectionComponent } from './components/contact-strip-section/contact-strip-section.component';

@Component({
  selector: 'app-home',
  imports: [
    NavbarComponent,
    FooterComponent,
    HeroSectionComponent,
    PopularProductsSectionComponent,
    StorySectionComponent,
    TestimonialsSectionComponent,
    ContactStripSectionComponent,
  ],
  templateUrl: './home.component.html',
})
export class HomeComponent {
  private productService = inject(ProductService);

  popularProducts: Product[] = [];

  constructor() {
    this.productService.getPopularProducts().subscribe({
      next: (products) => {
        this.popularProducts = products.filter((product) => product.active);
      },
      error: (error) => {
        console.error('Error cargando productos populares:', error);
      },
    });
  }
}
