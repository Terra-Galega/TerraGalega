import { Component, Input } from '@angular/core';

// Estrella dorada. El tamaño se pasa como clases de Tailwind.
// Se usa para marcar productos populares y en las reseñas.
@Component({
  selector: 'app-star-icon',
  templateUrl: './star-icon.component.html',
  styleUrl: './star-icon.component.scss',
})
export class StarIconComponent {
  // Tamaño de la estrella, como clases de Tailwind (ej. 'w-4 h-4')
  @Input() size = 'w-3 h-3';
}
