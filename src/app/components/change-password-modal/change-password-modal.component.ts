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

// Componente modal para cambiar la contraseña de un usuario.
@Component({
  selector: 'app-change-password-modal',
  imports: [ButtonComponent, IconButtonComponent, FormFieldComponent],
  templateUrl: './change-password-modal.component.html',
})
export class ChangePasswordModalComponent implements OnInit, OnDestroy {
  // Referencia al elemento HTML principal del componente.
  private host = inject<ElementRef<HTMLElement>>(ElementRef);

  // Mueve el modal al body para que quede por encima de otros elementos de la página.
  ngOnInit() {
    document.body.appendChild(this.host.nativeElement);
  }

  // Retira el elemento del modal del body cuando se destruye el componente.
  ngOnDestroy() {
    this.host.nativeElement.remove();
  }

  // Nombre de la persona a la que se le cambia la contraseña.
  @Input() userName = '';

  // Función proporcionada por el componente padre para guardar la nueva contraseña.
  @Input() save!: (current: string, next: string) => Observable<void>;

  // Notifica al componente padre cuando se solicita cerrar el modal.
  @Output() closed = new EventEmitter<void>();

  // Define los campos del formulario y el tipo de autocompletado correspondiente.
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

  // Almacena los valores ingresados en cada campo del formulario.
  values = { current: '', new: '', confirm: '' };

  // Controla si el contenido de cada campo se muestra como texto o contraseña.
  visible = { current: false, new: false, confirm: false };

  // Estado del proceso: mensaje de error, guardado en curso y resultado exitoso.
  errorMessage = '';
  saving = false;
  success = false;

  // Comprueba que la nueva contraseña tenga al menos seis caracteres.
  get hasMinLength() {
    return this.values.new.length >= 6;
  }

  // Verifica que la confirmación no esté vacía y coincida con la nueva contraseña.
  get matches() {
    return (
      this.values.confirm.length > 0 && this.values.new === this.values.confirm
    );
  }

  // Detecta si ambas contraseñas son diferentes después de ingresar la confirmación.
  get mismatch() {
    return (
      this.values.confirm.length > 0 && this.values.new !== this.values.confirm
    );
  }

  // Determina si el formulario cumple las condiciones para enviar la solicitud.
  get canSubmit() {
    return (
      !!this.values.current &&
      this.hasMinLength &&
      this.matches &&
      !this.saving &&
      !this.success
    );
  }

  // Alterna la visibilidad de la contraseña del campo seleccionado.
  toggle(key: 'current' | 'new' | 'confirm') {
    this.visible[key] = !this.visible[key];
  }

  // Valida los datos y solicita al servicio correspondiente cambiar la contraseña.
  submit() {
    if (!this.canSubmit) return;

    // Limpia el error anterior e indica que la operación está en curso.
    this.errorMessage = '';
    this.saving = true;

    // Ejecuta la función recibida del componente padre y procesa su respuesta.
    this.save(this.values.current, this.values.new).subscribe({
      // Actualiza el estado cuando el cambio de contraseña se completa correctamente.
      next: () => {
        this.saving = false;
        this.success = true;

        // Cierra el modal después de mostrar brevemente el mensaje de éxito.
        setTimeout(() => this.closed.emit(), 1200);
      },
      // Restablece el estado de guardado y muestra el error de la solicitud.
      error: (error) => {
        this.saving = false;
        this.errorMessage = getErrorMessage(
          error,
          'No se pudo cambiar la contraseña.',
        );
      },
    });
  }

  // Emite el evento de cierre para que el componente padre gestione el modal.
  close() {
    this.closed.emit();
  }
}
