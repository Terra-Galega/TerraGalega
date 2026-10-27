import { Component } from '@angular/core';
import { NavbarComponent } from '../../components/navbar/navbar.component';
import { FooterComponent } from '../../components/footer/footer.component';
import { AboutHeroSectionComponent } from './components/about-hero-section/about-hero-section.component';
import { HistorySectionComponent } from './components/history-section/history-section.component';
import { QuoteBannerComponent } from './components/quote-banner/quote-banner.component';
import { LocationSectionComponent } from './components/location-section/location-section.component';

@Component({
  selector: 'app-about-us',
  // Componentes que conforman las diferentes secciones de la página
  imports: [
    NavbarComponent,
    FooterComponent,
    AboutHeroSectionComponent,
    HistorySectionComponent,
    QuoteBannerComponent,
    LocationSectionComponent,
  ],
  templateUrl: './about-us.component.html',
})
export class AboutUsComponent {
  // Frase representativa de la tradición y la identidad del restaurante
  quote =
    'Nuestra mayor tradición no es solo conservar las recetas, sino mantener viva la pasión con la que se sirven.';

  // Imagen utilizada como acompañamiento visual de la frase
  quoteImage =
    'https://images.unsplash.com/photo-1773122150546-94699cfc8f23?q=80&w=735&auto=format&fit=crop&ixlib=rb-4.1.0&ixid=M3wxMjA3fDB8MHxwaG90by1wYWdlfHx8fGVufDB8fHx8fA%3D%3D';
}
