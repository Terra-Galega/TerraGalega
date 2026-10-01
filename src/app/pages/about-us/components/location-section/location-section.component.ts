import { Component } from '@angular/core';
import { ButtonComponent } from '../../../../components/button/button.component';
import {
  ContactInfoComponent,
  ContactInfo,
} from '../../../../components/contact-info/contact-info.component';
import { LocationMapComponent } from '../../../../components/location-map/location-map.component';

@Component({
  selector: 'app-location-section',
  imports: [ButtonComponent, ContactInfoComponent, LocationMapComponent],
  templateUrl: './location-section.component.html',
  styleUrl: './location-section.component.scss',
})
export class LocationSectionComponent {
  /** Dirección y teléfono: se pintan con <app-contact-info>. */
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

  /** URL "embed" del mapa: <app-location-map> se encarga de sanearla. */
  mapUrl =
    'https://www.google.com/maps/embed?pb=!1m18!1m12!1m3!1d3692.7843048189684!2d-74.05482151150028!3d4.676878070701616!2m3!1f0!2f0!3f0!3m2!1i1024!2i768!4f13.1!3m3!1m2!1s0x8e3f9a92bc5d5f05%3A0x6f031d8201a7f827!2sCl.%2093%20%2315-47%2C%20Bogot%C3%A1!5e0!3m2!1ses-419!2sco!4v1789465880652!5m2!1ses-419!2sco';
}
