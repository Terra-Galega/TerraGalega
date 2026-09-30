import { Component, Input } from '@angular/core';
import { FeatureKey } from '../../models/feature';

// Icono de una característica del plato. Los SVG viven solo aquí.
// El color se hereda (por defecto verde salvia) y el tamaño se pasa como clases.
@Component({
  selector: 'app-feature-icon',
  templateUrl: './feature-icon.component.html',
  styleUrl: './feature-icon.component.scss',
})
export class FeatureIconComponent {
  @Input({ required: true }) name!: FeatureKey;
  @Input() size = 'w-4 h-4';

  // Solape de la segunda llama en "picante intenso":
// -ml-1.5 en tamaño 3.5 (detalle), -ml-1 en el resto (tarjetas)
get overlap(): string {
  return this.size.includes('w-3.5') ? '-ml-1.5' : '-ml-1';
}
}