import { Component, Input } from '@angular/core';
import { fmtPct } from '../../utils/dashboard-metrics';

// Etiqueta de variación (▲ 12.4 % / ▼ 3 %) frente al periodo anterior.
// value = null cuando no hay base de comparación.
@Component({
  selector: 'app-delta-badge',
  template: `
    <span
      class="inline-flex items-center gap-1 text-[11px] font-semibold px-2 py-0.5 rounded-full"
      [style.background-color]="style.bg"
      [style.color]="style.color"
    >
      @if (value === null) {
        Nuevo
      } @else {
        <svg width="9" height="9" viewBox="0 0 10 10" fill="currentColor">
          @if (value >= 0) {
            <path d="M5 1l4 7H1z" />
          } @else {
            <path d="M5 9L1 2h8z" />
          }
        </svg>
        {{ pct }}
      }
    </span>
  `,
})
export class DeltaBadgeComponent {
  @Input({ required: true }) value: number | null = null;

  get pct(): string {
    return this.value === null ? '' : fmtPct(this.value);
  }

  // Verde si sube, rojo si baja, gris si no hay base o no cambia
  get style(): { bg: string; color: string } {
    if (this.value === null || this.value === 0) {
      return { bg: 'rgba(44, 44, 44, 0.08)', color: 'rgba(44, 44, 44, 0.6)' };
    }
    return this.value > 0
      ? { bg: 'rgba(85, 97, 65, 0.16)', color: '#556141' }
      : { bg: 'rgba(138, 34, 34, 0.12)', color: '#8a2222' };
  }
}