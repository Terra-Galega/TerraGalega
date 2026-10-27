import {
  Component,
  EventEmitter,
  HostListener,
  Input,
  Output,
} from '@angular/core';
import { ButtonComponent, ButtonVariant } from '../button/button.component';

// Diálogo reutilizable de confirmación o aviso; el padre controla su visibilidad y reacciona a sus eventos.
@Component({
  selector: 'app-modal',
  // Importa el botón compartido que se usa para las acciones de confirmar y cancelar.
  imports: [ButtonComponent],
  templateUrl: './modal.component.html',
})
export class ModalComponent {
  // Controla si el modal se muestra; lo decide el componente padre.
  @Input() open = false;
  // Título que se muestra en la parte superior del modal.
  @Input() title = '';
  // Texto principal que explica al usuario de qué trata el modal.
  @Input() message = '';
  // Texto del botón de confirmar; por defecto 'Aceptar'.
  @Input() confirmLabel = 'Aceptar';
  // Texto del botón de cancelar; si queda vacío, ese botón no se muestra.
  @Input() cancelLabel = '';
  // danger: acción destructiva (rojo) | error: aviso de error | primary: acción normal
  @Input() variant: 'danger' | 'error' | 'primary' = 'primary';
  // Lista de textos que se muestran como chips verdes debajo del mensaje
  // Lista de textos que se muestran como chips; si está vacía no se dibuja ningún chip.
  @Input() items: string[] = [];
  // Aviso destacado (amarillo) debajo de los chips
  // Texto del aviso; si queda vacío, el aviso no se muestra.
  @Input() warning = '';

  // Notifica al padre que el usuario pulsó el botón de confirmar.
  @Output() confirm = new EventEmitter<void>();
  // Notifica al padre que se quiere cerrar el modal (botón cancelar, clic fuera del cuadro o tecla Escape).
  @Output() cancel = new EventEmitter<void>();

  // Escucha la tecla Escape en todo el documento para permitir cerrar el modal con el teclado.
  @HostListener('document:keydown.escape')
  onEscape(): void {
    // Solo emite el evento si el modal está abierto, para no cancelar nada cuando está oculto.
    if (this.open) {
      this.cancel.emit();
    }
  }

  // Elige la variante del botón de confirmar según la variante del modal.
  get confirmVariant(): ButtonVariant {
    if (this.variant === 'danger') return 'danger';
    if (this.variant === 'error') return 'sage';
    return 'primary';
  }

  // Devuelve el color del título: oscuro en el modal normal y rojo en los de peligro o error.
  get titleColor(): string {
    return this.variant === 'primary' ? '#2c2c2c' : '#8a2222';
  }
}
