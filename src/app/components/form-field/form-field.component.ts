import {
  AfterContentInit,
  Component,
  ElementRef,
  Input,
  OnDestroy,
  inject,
} from '@angular/core';

// Contador para dar un id único a la etiqueta de cada campo
let nextId = 0;

// Controles que pueden recibir una etiqueta
const CONTROLS = 'input:not([type="hidden"]), select, textarea';

// Contenedor que muestra una etiqueta encima del campo de formulario que se proyecte dentro.
//
// Accesibilidad: la etiqueta se une a los controles de dentro con aria-labelledby, así el lector
// de pantalla dice "Precio, cuadro de texto" en vez de leer solo el placeholder. Se hace desde
// aquí para no tener que poner id/for a mano en cada formulario. Si un control ya tiene su
// propio nombre (aria-label, aria-labelledby o un <label>), se respeta.
// Un MutationObserver cubre los controles que aparecen después (por ejemplo dentro de un @if).
@Component({
  selector: 'app-form-field',
  templateUrl: './form-field.component.html',
  styleUrl: './form-field.component.scss',
})
export class FormFieldComponent implements AfterContentInit, OnDestroy {
  // Texto de la etiqueta; si queda vacío no se muestra.
  @Input() label = '';

  readonly labelId = `form-field-label-${nextId++}`;

  private host = inject(ElementRef<HTMLElement>).nativeElement as HTMLElement;
  private observer?: MutationObserver;

  ngAfterContentInit(): void {
    // Espera a que Angular pinte el contenido proyectado
    queueMicrotask(() => this.linkControls());
    if (typeof MutationObserver !== 'undefined') {
      this.observer = new MutationObserver(() => this.linkControls());
      this.observer.observe(this.host, { childList: true, subtree: true });
    }
  }

  ngOnDestroy(): void {
    this.observer?.disconnect();
  }

  // Al pulsar la etiqueta se enfoca el primer control (como haría un <label> normal)
  focusControl(): void {
    this.host.querySelector<HTMLElement>(CONTROLS)?.focus();
  }

  private linkControls(): void {
    if (!this.label) return;
    this.host.querySelectorAll<HTMLElement>(CONTROLS).forEach((control) => {
      const labelled =
        control.hasAttribute('aria-label') ||
        control.hasAttribute('aria-labelledby') ||
        ((control as HTMLInputElement).labels?.length ?? 0) > 0;
      if (!labelled) {
        control.setAttribute('aria-labelledby', this.labelId);
      }
    });
  }
}
