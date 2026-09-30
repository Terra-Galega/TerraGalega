import { Component, Input } from '@angular/core';

// Estrella dorada. El tamaño se pasa como clases de Tailwind.
@Component({
  selector: 'app-star-icon',
  templateUrl: './star-icon.component.html',
  styleUrl: './star-icon.component.scss',
})
export class StarIconComponent {
  @Input() size = 'w-3 h-3';
}