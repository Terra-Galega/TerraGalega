import { Component, inject } from '@angular/core';
import { DomSanitizer, SafeResourceUrl } from '@angular/platform-browser';
import { RouterLink } from '@angular/router';
import { NavbarComponent } from '../../components/navbar/navbar.component';
import { FooterComponent } from '../../components/footer/footer.component';
import {
  ContactInfoComponent,
  ContactInfo,
} from '../../components/contact-info/contact-info.component';

interface Stat {
  value: string;
  label: string;
}

@Component({
  selector: 'app-about-us',
  imports: [
    RouterLink,
    NavbarComponent,
    FooterComponent,
    ContactInfoComponent,
  ],
  templateUrl: './about-us.component.html',
  styleUrl: './about-us.component.scss',
})
export class AboutUsComponent {
  private sanitizer = inject(DomSanitizer);

  /** Estadísticas del restaurante (antes estaban escritas a mano en aboutUs.html). */
  stats: Stat[] = [
    { value: '6+', label: 'Años de trayectoria' },
    { value: '150+', label: 'Platos al día' },
    { value: '4.9★', label: 'Valoración promedio' },
  ];

  /** Dirección y teléfono: se pintan con el componente <app-contact-info>. */
  locationItems: ContactInfo[] = [
    {
      title: 'Dirección',
      value: 'Calle 93 #15-47, Zona Rosa, Bogotá',
      icon: 'location',
      link: 'https://www.google.com/maps/search/?api=1&query=Calle+93+%2315-47%2C+Bogot%C3%A1',
    },
    {
      title: 'Teléfono',
      value: '+57 601 456 7890',
      icon: 'phone',
    },
  ];

  /**
   * Angular bloquea por seguridad los <iframe src="..."> dinámicos,
   * por eso la URL del mapa se marca como confiable con el DomSanitizer.
   */
  mapUrl: SafeResourceUrl = this.sanitizer.bypassSecurityTrustResourceUrl(
    'https://www.google.com/maps/embed?pb=!1m18!1m12!1m3!1d3692.7843048189684!2d-74.05482151150028!3d4.676878070701616!2m3!1f0!2f0!3f0!3m2!1i1024!2i768!4f13.1!3m3!1m2!1s0x8e3f9a92bc5d5f05%3A0x6f031d8201a7f827!2sCl.%2093%20%2315-47%2C%20Bogot%C3%A1!5e0!3m2!1ses-419!2sco!4v1789465880652!5m2!1ses-419!2sco',
  );
}
