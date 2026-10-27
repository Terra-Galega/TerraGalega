import { Component, Input } from '@angular/core';

// Un dato de contacto que se muestra como tarjeta
export interface ContactInfo {
  title: string; // Nombre del dato (ej. "Dirección")
  value: string; // Valor que se muestra (ej. la dirección)
  icon: 'location' | 'phone' | 'email' | 'clock'; // Icono de la tarjeta
  link?: string; // Si se indica, la tarjeta es un enlace que abre en pestaña nueva
}

/**
 * Tarjetas con los datos de contacto del restaurante (dirección, horario y
 * teléfono). Por defecto muestra los datos del restaurante, pero la página
 * que lo use puede pasarle otra lista con [contactItems].
 */
@Component({
  selector: 'app-contact-info',
  imports: [],
  templateUrl: './contact-info.component.html',
  styleUrl: './contact-info.component.scss',
})
export class ContactInfoComponent {
  // Datos que se muestran. Si el padre no pasa nada, se usan estos.
  @Input() contactItems: ContactInfo[] = [
    {
      title: 'Dirección',
      value: 'Calle 93 #15-47, Zona Rosa, Bogotá',
      icon: 'location',
    },
    {
      title: 'Horario',
      value: 'Lun – Sáb: 12:00 – 22:00 · Dom: 12:00 – 20:00',
      icon: 'clock',
    },
    {
      title: 'Reservas',
      value: '+57 601 456 7890',
      icon: 'phone',
    },
  ];

  // grid: tarjetas en 3 columnas (pantallas medianas o más grandes)
  // stack: tarjetas una debajo de otra
  @Input() layout: 'grid' | 'stack' = 'grid';
}
