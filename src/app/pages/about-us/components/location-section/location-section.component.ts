import { TranslatePipe } from '../../../../pipes/translate.pipe';
import { Component } from '@angular/core';
import {
  ContactInfoComponent,
  ContactInfo,
} from '../../../../components/contact-info/contact-info.component';
import { LocationMapComponent } from '../../../../components/location-map/location-map.component';

@Component({
  selector: 'app-location-section',
  // Componentes utilizados para mostrar los datos de contacto y el mapa
  imports: [ContactInfoComponent, LocationMapComponent, TranslatePipe],
  templateUrl: './location-section.component.html',
  styleUrl: './location-section.component.scss',
})
export class LocationSectionComponent {
  // Información de contacto que se muestra mediante el componente reutilizable
  locationItems: ContactInfo[] = [
    {
      title: 'Dirección',
      value: 'Ak 15 # 92-81, Chapinero, Bogotá',
      icon: 'location',
      link: 'https://www.google.com/maps/search/?api=1&query=Ak+15+%23+92-81%2C+Chapinero%2C+Bogot%C3%A1',
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
      value: 'Lun – Sáb: 12:00 – 22:00 - Dom: 12:00 – 20:00',
      icon: 'clock',
    },
  ];

  // URL de inserción del mapa que muestra la ubicación del restaurante
  mapUrl =
    'https://www.google.com/maps/embed?pb=!1m18!1m12!1m3!1d1988.2672065576457!2d-74.05271797148316!3d4.6767543363447555!2m3!1f0!2f0!3f0!3m2!1i1024!2i768!4f13.1!3m3!1m2!1s0x8e3f9a92a2aceca3%3A0x1febcd9a02525fde!2zQWsgMTUgIyA5Mi04MSwgQ2hhcGluZXJvLCBCb2dvdMOhLCBELkMuLCBCb2dvdMOhLCBCb2dvdMOhLCBELkMu!5e0!3m2!1ses-419!2sco!4v1794690397857!5m2!1ses-419!2sco';
}
