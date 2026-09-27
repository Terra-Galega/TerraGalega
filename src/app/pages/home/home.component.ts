import { Component, inject } from '@angular/core';
import { FooterComponent } from '../../components/footer/footer.component';
import { NavbarComponent } from '../../components/navbar/navbar.component';
import { ProductService } from '../../services/product.service';
import { ProductCardComponent } from '../../components/product-card/product-card.component';
import { TestimonialsComponent } from './components/testimonial/testimonial.component';
import { ContactInfoComponent } from '../../components/contact-info/contact-info.component';
@Component({
  selector: 'app-home',
  imports: [
    NavbarComponent,
    FooterComponent,
    ProductCardComponent,
    TestimonialsComponent,
    ContactInfoComponent,
  ],
  templateUrl: './home.component.html',
  styleUrl: './home.component.scss',
})
export class HomeComponent {
  private productService = inject(ProductService);

  popularProducts = this.productService
    .getPopularProducts()
    .filter((product) => product.active);
}
