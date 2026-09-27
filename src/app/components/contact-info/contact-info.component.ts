import { Component, Input } from '@angular/core';

export interface ContactInfo {
  title: string;
  value: string;
  icon: 'location' | 'phone' | 'email' | 'clock';
  link?: string;
}

@Component({
  selector: 'app-contact-info',
  imports: [],
  templateUrl: './contact-info.component.html',
  styleUrl: './contact-info.component.scss'
})
export class ContactInfoComponent {

  @Input() contactItems: ContactInfo[] = [
    {
      title: 'Dirección',
      value: 'Calle 93 #15-47, Zona Rosa, Bogotá',
      icon: 'location'
    },
    {
      title: 'Horario',
      value: 'Lun – Sáb: 12:00 – 22:00 · Dom: 12:00 – 20:00',
      icon: 'clock'
    },
    {
      title: 'Reservas',
      value: '+57 601 456 7890',
      icon: 'phone'
    }
  ];

  @Input() layout: 'grid' | 'stack' = 'grid';

}