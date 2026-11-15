import { CdkTrapFocus } from '@angular/cdk/a11y';
import {
  AfterViewInit,
  Directive,
  ElementRef,
  Input,
  inject,
} from '@angular/core';

let nextId = 0;

// Convierte la ventana de un modal en un diálogo accesible. Se pone en el cuadro del modal
// (no en el fondo oscuro):  <div class="sheet-window ..." appDialog>
//
// - role="dialog" + aria-modal: el lector de pantalla anuncia "diálogo" y no lee lo de detrás.
// - Nombre del diálogo: el primer título (h1-h3) de dentro, con aria-labelledby. Si el modal no
//   tiene título, se puede dar uno con [dialogLabel]="'Texto'".
// - Foco (CdkTrapFocus): al abrir, el foco entra al modal; Tab y Shift+Tab no se salen de él;
//   al cerrar, vuelve al botón que lo abrió.
@Directive({
  selector: '[appDialog]',
  hostDirectives: [CdkTrapFocus],
  host: {
    '[attr.role]': 'role',
    '[attr.aria-modal]': '"true"',
    '[attr.aria-label]': 'dialogLabel || null',
  },
})
export class DialogDirective implements AfterViewInit {
  // "alertdialog" para avisos de error o confirmaciones importantes
  @Input() role: 'dialog' | 'alertdialog' = 'dialog';
  // Nombre del diálogo cuando no tiene un título visible
  @Input() dialogLabel = '';

  private el = inject(ElementRef<HTMLElement>).nativeElement as HTMLElement;

  constructor() {
    // Enfoca el modal al abrir y devuelve el foco al cerrar
    inject(CdkTrapFocus, { self: true }).autoCapture = true;
  }

  ngAfterViewInit(): void {
    if (this.dialogLabel || this.el.hasAttribute('aria-labelledby')) return;
    const heading = this.el.querySelector<HTMLElement>('h1, h2, h3');
    if (!heading) return;
    if (!heading.id) {
      heading.id = `dialog-title-${nextId++}`;
    }
    this.el.setAttribute('aria-labelledby', heading.id);
  }
}
