import { Component, inject } from '@angular/core';
import { FooterComponent } from '../../components/footer/footer.component';
import { NavbarComponent } from '../../components/navbar/navbar.component';
import { ProductService } from '../../services/product.service';
import { AnnouncementService } from '../../services/announcement.service';
import { Product } from '../../models/product';
import { Announcement } from '../../models/announcement';
import { HeroSectionComponent } from './components/hero-section/hero-section.component';
import { PopularProductsSectionComponent } from './components/popular-products-section/popular-products-section.component';
import { StorySectionComponent } from './components/story-section/story-section.component';
import { TestimonialsSectionComponent } from './components/testimonials-section/testimonials-section.component';
import { AnnouncementsSectionComponent } from './components/announcements-section/announcements-section.component';

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
    AnnouncementsSectionComponent,
  ],
  templateUrl: './home.component.html',
})
// Carga los productos populares y los anuncios y se los pasa a cada sección.
export class HomeComponent {
  // Servicios para consultar al backend.
  private productService = inject(ProductService);
  private announcementService = inject(AnnouncementService);

  // Productos que se muestran en la sección de favoritos; inicia vacío hasta que responda el backend.
  popularProducts: Product[] = [];

  // Anuncios del carrusel (máximo 3); si no hay ninguno, se muestra la franja de contacto.
  announcements: Announcement[] = [];

  // Al crear el componente se solicitan los productos populares y los anuncios.
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

    this.announcementService.getAnnouncements().subscribe({
      next: (announcements) => {
        this.announcements = announcements;
      },
      // Si falla, queda la franja de contacto de siempre.
      error: (error) => {
        console.error('Error cargando anuncios:', error);
      },
    });
  }
}