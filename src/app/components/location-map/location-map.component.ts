import { Component, inject, Input } from '@angular/core';
import { DomSanitizer, SafeResourceUrl } from '@angular/platform-browser';

// Único origen permitido para el mapa: los "embed" de Google Maps
const GOOGLE_MAPS_EMBED = 'https://www.google.com/maps/embed';

// Mapa de Google incrustado dentro de una tarjeta de vidrio.
// Angular bloquea los <iframe src="..."> dinámicos, por eso la URL se marca
// como confiable aquí adentro y las páginas solo pasan el texto de la URL.
@Component({
  selector: 'app-location-map',
  templateUrl: './location-map.component.html',
  styleUrl: './location-map.component.scss',
})
export class LocationMapComponent {
  // Herramienta de Angular para marcar una URL como segura
  private sanitizer = inject(DomSanitizer);

  // URL ya marcada como confiable; si es null, el mapa no se dibuja
  safeUrl: SafeResourceUrl | null = null;

  // URL "embed" de Google Maps (Compartir → Insertar un mapa)
  @Input({ required: true }) set src(value: string) {
    // Solo se confía en URLs de embed de Google Maps, no en cualquier texto
    this.safeUrl = value.startsWith(GOOGLE_MAPS_EMBED)
      ? this.sanitizer.bypassSecurityTrustResourceUrl(value)
      : null;
  }

  // Título del iframe, para accesibilidad
  @Input() mapTitle = 'Ubicación en el mapa';
}
