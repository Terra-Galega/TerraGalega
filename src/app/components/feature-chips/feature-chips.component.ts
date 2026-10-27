import { Component, Input } from '@angular/core';
import { FEATURES, FeatureKey } from '../../models/feature';
import { FeatureIconComponent } from '../feature-icon/feature-icon.component';

// Chips de características. El contenedor (flex, gap, alto mínimo) lo define
// quien lo usa con clases sobre <app-feature-chips>.
// Solo muestra los chips de las características que el producto tiene en true.
@Component({
  selector: 'app-feature-chips',
  imports: [FeatureIconComponent],
  templateUrl: './feature-chips.component.html',
  styleUrl: './feature-chips.component.scss',
})
export class FeatureChipsComponent {
  // Producto (o cualquier objeto) con las características como true/false
  @Input({ required: true }) product!: Record<FeatureKey, boolean>;

  // sm: tarjetas (etiquetas cortas) · md: detalle del plato (etiquetas completas)
  @Input() size: 'sm' | 'md' = 'sm';

  // Lista de todas las características posibles, para recorrerla en el HTML
  readonly features = FEATURES;
}
