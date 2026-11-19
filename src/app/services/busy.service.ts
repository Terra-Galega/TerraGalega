import { DOCUMENT } from '@angular/common';
import { Injectable, computed, effect, inject, signal } from '@angular/core';

// Elementos que cuentan como "botón" para el bloqueo global
const BUTTON_SELECTOR = 'button, [role="button"], input[type="submit"], input[type="button"]';

// Atributo para que un botón concreto siga funcionando durante una petición
// (por ejemplo, un "Cerrar" que no envía nada). Uso: <button data-busy-ignore>
const IGNORE_SELECTOR = '[data-busy-ignore]';

// Tiempo antes de mostrar los botones como deshabilitados. Las peticiones rápidas
// (< 150 ms) bloquean igual los clics, pero sin parpadeo visual en toda la página.
const VISIBLE_DELAY_MS = 150;

// Lleva la cuenta de las peticiones que crean, editan o borran (POST, PUT, PATCH,
// DELETE) que siguen en curso. Mientras haya alguna:
// - app-button y app-icon-button se deshabilitan de verdad (atributo disabled).
// - Los <button> normales y los envíos de formulario se frenan aquí mismo, antes
//   de que Angular los reciba, así un doble clic o un Enter repetido no envía dos veces.
// El contador lo mueve busyInterceptor; las peticiones de fondo se excluyen con SKIP_BUSY.
@Injectable({ providedIn: 'root' })
export class BusyService {
  private document = inject(DOCUMENT);

  private readonly pending = signal(0);
  private visibleTimer?: ReturnType<typeof setTimeout>;

  // true en cuanto empieza una petición: es lo que bloquea los clics
  readonly busy = computed(() => this.pending() > 0);
  // true solo si la petición tarda más de 150 ms: es lo que se ve deshabilitado
  readonly busyVisible = signal(false);

  constructor() {
    // Fase de captura: se ejecuta antes que los (click) y (submit) de los componentes
    this.document.addEventListener('click', (event) => this.block(event), true);
    this.document.addEventListener('submit', (event) => this.block(event), true);

    // Marca <html data-busy> para el estilo de los <button> normales (styles.scss)
    effect(() => {
      const root = this.document.documentElement;
      if (this.busyVisible()) {
        root.setAttribute('data-busy', '');
        root.setAttribute('aria-busy', 'true');
      } else {
        root.removeAttribute('data-busy');
        root.removeAttribute('aria-busy');
      }
    });
  }

  start(): void {
    this.pending.update((count) => count + 1);
    if (this.pending() === 1) {
      clearTimeout(this.visibleTimer);
      this.visibleTimer = setTimeout(() => this.busyVisible.set(this.busy()), VISIBLE_DELAY_MS);
    }
  }

  end(): void {
    this.pending.update((count) => Math.max(0, count - 1));
    if (this.pending() === 0) {
      clearTimeout(this.visibleTimer);
      this.busyVisible.set(false);
    }
  }

  // Cancela clics en botones y envíos de formularios mientras haya una petición en curso
  private block(event: Event): void {
    if (!this.busy()) return;

    const target = event.target;
    if (!(target instanceof Element) || target.closest(IGNORE_SELECTOR)) return;

    const isSubmit = event.type === 'submit';
    if (isSubmit || target.closest(BUTTON_SELECTOR)) {
      event.preventDefault();
      event.stopImmediatePropagation();
    }
  }
}
