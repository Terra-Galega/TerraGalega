import { Component } from '@angular/core';
import { RouterLink } from '@angular/router';
import { NavbarComponent } from '../../components/navbar/navbar.component';
import { FooterComponent } from '../../components/footer/footer.component';

// Página pública: Meta exige su URL para publicar la app de WhatsApp.
// Los plazos de conservación deben coincidir con WhatsAppRetention en el backend.
@Component({
  selector: 'app-privacy',
  imports: [NavbarComponent, FooterComponent, RouterLink],
  templateUrl: './privacy.component.html',
})
export class PrivacyComponent {
  readonly updated = '23 de noviembre de 2026';
  readonly email = 'terragalega0@gmail.com';
}
