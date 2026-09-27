import { Component } from '@angular/core';
import { NavbarComponent } from '../../components/navbar/navbar.component';
import { FooterComponent } from '../../components/footer/footer.component';
import {ContactInfoComponent,ContactInfo} from '../../components/contact-info/contact-info.component';

@Component({
  selector: 'app-contact',
  imports: [
    NavbarComponent,
    FooterComponent,
    ContactInfoComponent
  ],
  templateUrl: './contact.component.html',
  styleUrl: './contact.component.scss'
})
export class ContactComponent {

  contactItems: ContactInfo[] = [
    {
      title: 'Dirección',
      value: 'Calle 93 #15-47, Zona Rosa, Bogotá',
      icon: 'location',
      link: 'https://www.google.com/maps/search/?api=1&query=Calle+93+%2315-47%2C+Bogot%C3%A1'
    },
    {
      title: 'Teléfono',
      value: '+57 601 456 7890',
      icon: 'phone'
    },
    {
      title: 'Correo',
      value: 'contacto@terragalega.co',
      icon: 'email'
    },
    {
      title: 'Horario',
      value: 'Lun – Sáb: 12:00 – 22:00 · Dom: 12:00 – 20:00',
      icon: 'clock'
    }
  ];
}