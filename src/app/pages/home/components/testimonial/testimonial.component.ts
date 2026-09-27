import { Component } from '@angular/core';

interface Testimonial {
  name: string;
  role: string;
  text: string;
}

@Component({
  selector: 'app-testimonial',
  imports: [],
  templateUrl: './testimonial.component.html',
  styleUrl: './testimonial.component.scss'
})
export class TestimonialsComponent {

  testimonials: Testimonial[] = [
    {
      name: 'María García',
      role: 'Cliente fiel',
      text: 'El pulpo a la gallega es simplemente extraordinario. Me transporta directamente a Galicia con cada bocado. El ambiente cálido y el servicio impecable hacen de cada visita una experiencia única.'
    },
    {
      name: 'Carlos Rodríguez',
      role: 'Crítico gastronómico',
      text: 'Terra Galega es la joya gastronómica de la ciudad. La paella de mariscos es perfecta en sabor y textura. Ingredientes de primera calidad con técnica y pasión en cada preparación.'
    },
    {
      name: 'Lucía Martínez',
      role: 'Food blogger',
      text: 'Desde las croquetas hasta la tarta de Santiago, cada plato cuenta la historia de Galicia. El servicio es cálido, la ambientación preciosa y los sabores completamente auténticos.'
    }
  ];
}