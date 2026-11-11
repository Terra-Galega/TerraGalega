import { Component, Input } from '@angular/core';
import { FeatureKey } from '../../models/feature';

// Icono de una característica del plato (vegetariano, picante, alérgenos…).
// Hereda el color del texto (currentColor), así que el color lo define quien lo usa.
// Uso: <app-feature-icon name="vegetarian" size="w-4 h-4" />
@Component({
  selector: 'app-feature-icon',
  template: `
    <svg
      [class]="size + ' flex-shrink-0'"
      viewBox="0 0 24 24"
      fill="none"
      stroke="currentColor"
      stroke-width="1.8"
      stroke-linecap="round"
      stroke-linejoin="round"
      aria-hidden="true"
    >
      @switch (name) {
        @case ('vegetarian') {
          <!-- Hoja -->
          <path d="M5 19c0-8 5-14 15-14 0 10-6 15-14 15" />
          <path d="M5 19c3-5 6-8 10-10" />
        }
        @case ('spicyMild') {
          <!-- Una llama -->
          <path
            d="M12 3c1 3.5 5 5.5 5 10a5 5 0 01-10 0c0-2 1-3.2 2-4.2 0 1.5.7 2.3 1.5 2.7C10.3 8.5 10.8 5.5 12 3z"
          />
        }
        @case ('spicyHot') {
          <!-- Llama doble -->
          <path
            d="M9 3c.8 2.8 4 4.4 4 8a4 4 0 01-8 0c0-1.6.8-2.6 1.6-3.4 0 1.2.6 1.8 1.2 2.1C7.6 7.2 8.2 5.2 9 3z"
          />
          <path
            d="M16 9c.6 2 3 3.2 3 5.8a3 3 0 01-6 0c0-1.2.6-1.9 1.2-2.5 0 .9.5 1.4.9 1.6-.2-1.8.3-3.3.9-4.9z"
          />
        }
        @case ('containsNuts') {
          <!-- Nuez / bellota -->
          <path d="M6 10h12c0 5-2.5 9-6 10-3.5-1-6-5-6-10z" />
          <path d="M5 10c0-2.2 3-4 7-4s7 1.8 7 4" />
          <path d="M12 6V3" />
        }
        @case ('containsSeafood') {
          <!-- Pez -->
          <path d="M3 12c3-4.5 8-5.5 12-3l6-3v12l-6-3c-4 2.5-9 1.5-12-3z" />
          <circle cx="8" cy="11" r="0.8" fill="currentColor" stroke="none" />
        }
        @case ('containsGluten') {
          <!-- Espiga de trigo -->
          <path d="M12 21V8" />
          <path d="M12 8c-2.5 0-4-1.5-4-4 2.5 0 4 1.5 4 4z" />
          <path d="M12 8c2.5 0 4-1.5 4-4-2.5 0-4 1.5-4 4z" />
          <path d="M12 14c-2.5 0-4-1.5-4-4 2.5 0 4 1.5 4 4z" />
          <path d="M12 14c2.5 0 4-1.5 4-4-2.5 0-4 1.5-4 4z" />
          <path d="M12 20c-2.5 0-4-1.5-4-4 2.5 0 4 1.5 4 4z" />
          <path d="M12 20c2.5 0 4-1.5 4-4-2.5 0-4 1.5-4 4z" />
        }
      }
    </svg>
  `,
})
export class FeatureIconComponent {
  // Característica que se dibuja
  @Input({ required: true }) name!: FeatureKey;
  // Tamaño como clases de Tailwind
  @Input() size = 'w-4 h-4';
}
