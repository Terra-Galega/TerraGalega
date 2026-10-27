import {
  Component,
  EventEmitter,
  HostListener,
  Input,
  Output,
} from '@angular/core';
import { ButtonComponent, ButtonVariant } from '../button/button.component';

@Component({
  selector: 'app-modal',
  imports: [ButtonComponent],
  templateUrl: './modal.component.html',
})
export class ModalComponent {
  @Input() open = false;
  @Input() title = '';
  @Input() message = '';
  @Input() confirmLabel = 'Aceptar';
  @Input() cancelLabel = '';
  // danger: acción destructiva (rojo) | error: aviso de error | primary: acción normal
  @Input() variant: 'danger' | 'error' | 'primary' = 'primary';
  // Lista de textos que se muestran como chips verdes debajo del mensaje
  @Input() items: string[] = [];
  // Aviso destacado (amarillo) debajo de los chips
  @Input() warning = '';

  @Output() confirm = new EventEmitter<void>();
  @Output() cancel = new EventEmitter<void>();

  @HostListener('document:keydown.escape')
  onEscape(): void {
    if (this.open) {
      this.cancel.emit();
    }
  }

  get confirmVariant(): ButtonVariant {
    if (this.variant === 'danger') return 'danger';
    if (this.variant === 'error') return 'sage';
    return 'primary';
  }

  get titleColor(): string {
    return this.variant === 'primary' ? '#2c2c2c' : '#8a2222';
  }
}