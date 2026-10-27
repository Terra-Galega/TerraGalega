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

// Página de inicio: ensambla el navbar, las secciones del home y el footer.
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
// solo carga los productos populares y se los pasa a la sección correspondiente.
export class HomeComponent {
  
  // Servicio para consultar los productos al backend.
  private productService = inject(ProductService);

  // Productos que se muestran en la sección de favoritos; inicia vacío hasta que responda el backend.
  popularProducts: Product[] = [];

  // Al crear el componente se solicitan los productos populares.
  constructor() {
    this.productService.getPopularProducts().subscribe({
      // Se conservan solo los productos activos para no mostrar platos deshabilitados en el home.
      next: (products) => {
        this.popularProducts = products.filter((product) => product.active);
      },
      // Si la carga falla solo se registra el error en consola y la sección queda sin tarjetas.
      error: (error) => {
        console.error('Error cargando productos populares:', error);
      },
    });
  }
}