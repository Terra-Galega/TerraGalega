import { Component } from '@angular/core';
import { RouterLink } from '@angular/router';

// Sección Nuestra Historia del home: texto a un lado e imagen con estadísticas al otro.
@Component({
  selector: 'app-story-section',
  // RouterLink permite el enlace Conocer más hacia la página Sobre nosotros.
  imports: [RouterLink],
  templateUrl: './story-section.component.html',
  styleUrl: './story-section.component.scss',
})
export class StorySectionComponent {
  // Datos de la tarjeta flotante sobre la imagen; value es el número mostrado y label su descripción.
  stats = [
    { value: '6+', label: 'Años' },
    { value: '150+', label: 'Platos/día' },
    { value: '4.9★', label: 'Valoración' },
  ];
}