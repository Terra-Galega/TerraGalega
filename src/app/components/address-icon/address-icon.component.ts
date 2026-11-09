import { Component, Input } from '@angular/core';
import { AddressIconKind, addressIconKind } from '../../utils/address-icon';

// Ícono de una dirección: lo elige según el nombre, o se fuerza con `kind`.
@Component({
  selector: 'app-address-icon',
  host: { class: 'inline-flex' },
  template: `
    <svg
      class="h-5 w-5"
      viewBox="0 0 24 24"
      fill="none"
      stroke="currentColor"
      stroke-width="1.8"
      stroke-linecap="round"
      stroke-linejoin="round"
      aria-hidden="true"
    >
      @switch (icon) {
        @case ('home') {
          <path
            d="M3 11.5L12 4l9 7.5M5.5 10v9.5a1 1 0 001 1H10v-5.5h4v5.5h3.5a1 1 0 001-1V10"
          />
        }
        @case ('work') {
          <path
            d="M4 7.5h16a1 1 0 011 1V19a1 1 0 01-1 1H4a1 1 0 01-1-1V8.5a1 1 0 011-1zM9 7.5V5.5a1 1 0 011-1h4a1 1 0 011 1v2M3 13h18"
          />
        }
        @case ('building') {
          <path
            d="M6 20.5V5a1 1 0 011-1h7a1 1 0 011 1v15.5M15 9.5h2a1 1 0 011 1v10M3.5 20.5h17M9.5 8h2M9.5 12h2M9.5 16h2"
          />
        }
        @case ('family') {
          <path
            d="M12 20s-7-4.4-7-10a4 4 0 017-2.6A4 4 0 0119 10c0 5.6-7 10-7 10z"
          />
        }
        @case ('school') {
          <path
            d="M2.5 9L12 4.5 21.5 9 12 13.5 2.5 9zM6 11.2V16c0 1.4 2.7 3 6 3s6-1.6 6-3v-4.8M21.5 9v5.5"
          />
        }
        @case ('plus') {
          <path d="M12 5v14M5 12h14" />
        }
        @default {
          <path
            d="M12 21s-7-6-7-11.2a7 7 0 1114 0C19 15 12 21 12 21zM12 12.6a2.6 2.6 0 100-5.2 2.6 2.6 0 000 5.2z"
          />
        }
      }
    </svg>
  `,
})
export class AddressIconComponent {
  // Nombre de la dirección; de él se deduce el ícono
  @Input() name = '';

  // Si se indica, se usa este ícono en vez de deducirlo del nombre
  @Input() kind: AddressIconKind | null = null;

  get icon(): AddressIconKind {
    return this.kind ?? addressIconKind(this.name);
  }
}
