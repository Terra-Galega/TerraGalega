import { Component, inject, isDevMode } from '@angular/core';
import { CELEBRATIONS } from '../celebrations.data';
import { CelebrationService } from '../celebration.service';

// Botón flotante para ver cómo queda cada festividad sin esperar a su fecha.
// Solo aparece en desarrollo; para mostrarlo siempre, cambia visible a true.
@Component({
  selector: 'app-celebration-switcher',
  templateUrl: './celebration-switcher.component.html',
  styleUrl: './celebration-switcher.component.scss',
})
export class CelebrationSwitcherComponent {
  private celebrations = inject(CelebrationService);

  readonly visible = isDevMode();

  // Opción elegida ('' es la fecha real)
  selected = () => this.celebrations.preview() ?? '';

  options = [
    { id: '', name: 'Usar fecha real' },
    { id: 'none', name: 'Sin efecto' },
    ...CELEBRATIONS.map(({ id, name }) => ({ id, name })),
  ];

  menuOpen = false;

  choose(id: string): void {
    this.celebrations.setPreview(id);
    this.menuOpen = false;
  }
}
