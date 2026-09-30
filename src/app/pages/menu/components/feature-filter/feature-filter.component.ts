import { Component, EventEmitter, Input, Output } from '@angular/core';
import { ButtonComponent } from '../../../../components/button/button.component';
import { CheckOptionComponent } from '../../../../components/check-option/check-option.component';
import { FeatureIconComponent } from '../../../../components/feature-icon/feature-icon.component';
import { FEATURES, FeatureKey } from '../../../../models/feature';

// Botón con panel de filtros por característica. Las casillas son temporales
// hasta pulsar "Aplicar"; solo entonces se emite el resultado al padre.
@Component({
  selector: 'app-feature-filter',
  imports: [ButtonComponent, CheckOptionComponent, FeatureIconComponent],
  templateUrl: './feature-filter.component.html',
  styleUrl: './feature-filter.component.scss',
})
export class FeatureFilterComponent {
  readonly features = FEATURES;

  open = false;
  selected = new Set<FeatureKey>();
  private applied: FeatureKey[] = [];

  // Filtros ya aplicados (viene del padre). Si el padre los reinicia, las casillas también.
  @Input() set active(value: FeatureKey[]) {
    this.applied = value;
    this.selected = new Set(value);
  }

  @Output() filtersApplied = new EventEmitter<FeatureKey[]>();

  get hasActive(): boolean {
    return this.applied.length > 0;
  }

  toggle(key: FeatureKey): void {
    if (this.selected.has(key)) this.selected.delete(key);
    else this.selected.add(key);
  }

  clear(): void {
    this.selected = new Set();
    this.filtersApplied.emit([]);
  }

  apply(): void {
    this.open = false;
    this.filtersApplied.emit([...this.selected]);
  }
}