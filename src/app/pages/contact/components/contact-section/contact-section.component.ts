import { Component } from '@angular/core';
import { PageHeaderComponent } from '../../../../components/page-header/page-header.component';
import {
  ContactInfoComponent,
  ContactInfo,
} from '../../../../components/contact-info/contact-info.component';
import { ContactFormComponent } from '../contact-form/contact-form.component';

@Component({
  selector: 'app-contact-section',
  imports: [PageHeaderComponent, ContactInfoComponent, ContactFormComponent],
  templateUrl: './contact-section.component.html',
  styleUrl: './contact-section.component.scss',
})
export class ContactSectionComponent {
  /** Datos del restaurante: se pintan con <app-contact-info>. */
  contactItems: ContactInfo[] = [
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
    {
      title: 'Correo',
      value: 'contacto@terragalega.co',
      icon: 'email',
    },
    {
      title: 'Horario',
      value: 'Lun – Sáb: 12:00 – 22:00 · Dom: 12:00 – 20:00',
      icon: 'clock',
    },
  ];
}
