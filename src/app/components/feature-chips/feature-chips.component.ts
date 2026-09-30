import { Component, Input } from '@angular/core';
import { FEATURES, FeatureKey } from '../../models/feature';
import { FeatureIconComponent } from '../feature-icon/feature-icon.component';

// Chips de características. El contenedor lo define
// quien lo usa con clases sobre <app-feature-chips>.
@Component({
  selector: 'app-feature-chips',
  imports: [FeatureIconComponent],
  templateUrl: './feature-chips.component.html',
  styleUrl: './feature-chips.component.scss',
})
export class FeatureChipsComponent {
  @Input({ required: true }) product!: Record<FeatureKey, boolean>;

  readonly features = FEATURES;
}