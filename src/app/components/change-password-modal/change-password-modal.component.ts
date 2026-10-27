import {
  Component,
  ElementRef,
  EventEmitter,
  Input,
  OnDestroy,
  OnInit,
  Output,
  inject,
} from '@angular/core';
import { Observable } from 'rxjs';
import { ButtonComponent } from '../button/button.component';
import { IconButtonComponent } from '../icon-button/icon-button.component';
import { FormFieldComponent } from '../form-field/form-field.component';
import { getErrorMessage } from '../../utils/http-error';

@Component({
  selector: 'app-change-password-modal',
  imports: [ButtonComponent, IconButtonComponent, FormFieldComponent],
  templateUrl: './change-password-modal.component.html',
})
export class ChangePasswordModalComponent implements OnInit, OnDestroy {
  private host = inject<ElementRef<HTMLElement>>(ElementRef);

  // Se mueve al <body> para que quede por encima de cualquier modal o lista
  ngOnInit() {
    document.body.appendChild(this.host.nativeElement);
  }

  ngOnDestroy() {
    this.host.nativeElement.remove();
  }
  // Nombre de la persona a la que se le cambia la contraseña
  @Input() userName = '';
  // El padre decide a qué servicio llamar (operador, cliente o domiciliario)
  @Input() save!: (current: string, next: string) => Observable<void>;

  @Output() closed = new EventEmitter<void>();

  readonly fields = [
    {
      key: 'current',
      label: 'Contraseña actual',
      autocomplete: 'current-password',
    },
    { key: 'new', label: 'Nueva contraseña', autocomplete: 'new-password' },
    {
      key: 'confirm',
      label: 'Confirmar nueva contraseña',
      autocomplete: 'new-password',
    },
  ] as const;

  values = { current: '', new: '', confirm: '' };
  visible = { current: false, new: false, confirm: false };

  errorMessage = '';
  saving = false;
  success = false;

  get hasMinLength() {
    return this.values.new.length >= 6;
  }
  get matches() {
    return (
      this.values.confirm.length > 0 && this.values.new === this.values.confirm
    );
  }
  get mismatch() {
    return (
      this.values.confirm.length > 0 && this.values.new !== this.values.confirm
    );
  }

  get canSubmit() {
    return (
      !!this.values.current &&
      this.hasMinLength &&
      this.matches &&
      !this.saving &&
      !this.success
    );
  }

  toggle(key: 'current' | 'new' | 'confirm') {
    this.visible[key] = !this.visible[key];
  }

  submit() {
    if (!this.canSubmit) return;

    this.errorMessage = '';
    this.saving = true;

    this.save(this.values.current, this.values.new).subscribe({
      next: () => {
        this.saving = false;
        this.success = true;
        setTimeout(() => this.closed.emit(), 1200);
      },
      error: (error) => {
        this.saving = false;
        this.errorMessage =
          getErrorMessage(error, 'No se pudo cambiar la contraseña.');
      },
    });
  }

  close() {
    this.closed.emit();
  }
}