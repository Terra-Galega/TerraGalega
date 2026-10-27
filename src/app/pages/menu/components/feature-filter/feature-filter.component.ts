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
  // Lista de características disponibles, una casilla por cada una
  readonly features = FEATURES;

  // Si el panel está abierto o cerrado
  open = false;
  // Casillas marcadas en este momento (todavía no aplicadas)
  selected = new Set<FeatureKey>();
  // Filtros que ya están aplicados en el menú
  private applied: FeatureKey[] = [];

  // Filtros ya aplicados (viene del padre). Si el padre los reinicia, las casillas también.
  @Input() set active(value: FeatureKey[]) {
    this.applied = value;
    this.selected = new Set(value);
  }

  // Se emite la lista de características elegidas al aplicar o limpiar
  @Output() filtersApplied = new EventEmitter<FeatureKey[]>();

  // true si hay filtros aplicados; sirve para resaltar el botón
  get hasActive(): boolean {
    return this.applied.length > 0;
  }

  // Marca o desmarca una característica (solo cambia la selección temporal)
  toggle(key: FeatureKey): void {
    if (this.selected.has(key)) this.selected.delete(key);
    else this.selected.add(key);
  }

  // Desmarca todo y avisa de inmediato al padre, sin esperar a "Aplicar"
  clear(): void {
    this.selected = new Set();
    this.filtersApplied.emit([]);
  }

  // Cierra el panel y envía las características marcadas al padre
  apply(): void {
    this.open = false;
    this.filtersApplied.emit([...this.selected]);
  }
}
