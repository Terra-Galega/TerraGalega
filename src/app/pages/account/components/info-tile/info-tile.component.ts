import { Component, Input } from '@angular/core';

// Fila de un dato (icono + etiqueta + valor). El icono se proyecta dentro
// y debe usar stroke="currentColor" para tomar el color del tono.
@Component({
  selector: 'app-info-tile',
  templateUrl: './info-tile.component.html',
  host: { class: 'block' },
})
export class InfoTileComponent {
  @Input() label = '';
  @Input() value = '';
  @Input() tone: 'terra' | 'gold' = 'terra';

  get iconBg(): string {
    return this.tone === 'gold'
      ? 'rgba(217, 154, 33, 0.15)'
      : 'rgba(178, 87, 31, 0.1)';
  }
}
