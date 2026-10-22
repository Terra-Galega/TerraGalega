import {
  Component,
  EventEmitter,
  HostListener,
  Input,
  Output,
} from '@angular/core';
import { ButtonComponent, ButtonVariant } from '../button/button.component';

export function getErrorMessage(
    error: any,
    fallback = 'Ocurrió un error inesperado.',
): string {
    const body = error?.error;
    if (typeof body === 'string' && body.trim()) return body;
    return body?.message ?? body?.error ?? error?.message ?? fallback;
}

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
