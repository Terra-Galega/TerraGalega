import { Component, EventEmitter, Input, Output } from '@angular/core';

export interface SegmentOption {
  value: string;
  label: string;
}

// Selector de opciones en forma de píldoras (Hoy / 7 días / 30 días, Barras / Línea…).
// Mismo estilo que los filtros "Todos / Activos / Completados" de Órdenes.
@Component({
  selector: 'app-segmented-control',
  template: `
    <div class="inline-flex gap-1 p-1 rounded-xl" style="background-color: rgba(44, 44, 44, 0.06)">
      @for (option of options; track option.value) {
        <button
          type="button"
          class="text-(length:--dfs-12) font-medium px-3 py-1.5 rounded-lg"
          style="transition: all 0.2s ease"
          [style.background-color]="value === option.value ? '#556141' : 'transparent'"
          [style.color]="value === option.value ? '#f5f2eb' : 'rgba(44, 44, 44, 0.55)'"
          [style.box-shadow]="value === option.value ? '0 2px 8px rgba(85, 97, 65, 0.3)' : 'none'"
          (click)="valueChange.emit(option.value)"
        >
          {{ option.label }}
        </button>
      }
    </div>
  `,
})
export class SegmentedControlComponent {
  @Input({ required: true }) options: SegmentOption[] = [];
  @Input({ required: true }) value = '';
  @Output() valueChange = new EventEmitter<string>();
}