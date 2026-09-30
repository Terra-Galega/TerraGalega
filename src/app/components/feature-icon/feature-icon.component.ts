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
}