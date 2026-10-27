import { Component, Input } from '@angular/core';

// Banner que muestra una frase destacada sobre una imagen de fondo oscurecida.
@Component({
  selector: 'app-quote-banner',
  templateUrl: './quote-banner.component.html',
  styleUrl: './quote-banner.component.scss',
})
export class QuoteBannerComponent {
  // Frase que se presenta como mensaje destacado en el banner
  @Input({ required: true }) quote = '';

  // Ruta de la imagen utilizada como fondo
  @Input({ required: true }) image = '';

  // Texto alternativo de la imagen para mejorar la accesibilidad
  @Input() imageAlt = '';
}
