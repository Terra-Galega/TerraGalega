import { Component, Input } from '@angular/core';
import { FeatureKey } from '../../models/feature';

// Icono de una característica del plato (vegetariano, picante, alérgenos…). Son los mismos
// dibujos de la app móvil. El color se hereda (currentColor) y el tamaño llega como clases.
// Uso: <app-feature-icon name="vegetarian" size="w-4 h-4" />
@Component({
  selector: 'app-feature-icon',
  templateUrl: './feature-icon.component.html',
  styleUrl: './feature-icon.component.scss',
})
export class FeatureIconComponent {
  // Característica que se dibuja
  @Input({ required: true }) name!: FeatureKey;
  // Tamaño como clases de Tailwind
  @Input() size = 'w-4 h-4';
}
