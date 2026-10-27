import {
  Component,
  EventEmitter,
  inject,
  Input,
  Output,
  SimpleChanges,
} from '@angular/core';
import { FormFieldComponent } from '../../../../components/form-field/form-field.component';
import { IconButtonComponent } from '../../../../components/icon-button/icon-button.component';
import { OperatorService } from '../../../../services/operator.service';
import { CreateOperator, Operator, Role } from '../../../../models/operator';
import { ButtonComponent } from '../../../../components/button/button.component';
import { ToastService } from '../../../../services/toast.service';
import { getErrorMessage } from '../../../../utils/http-error';
import { EMAIL_ERROR_MESSAGE, isValidEmail } from '../../../../utils/email';
// Modal para crear o editar un operador: si recibe un operador funciona como edición; si no, como creación.
@Component({
  selector: 'app-admin-operator-modal',
  imports: [
    IconButtonComponent,
    IconButtonComponent,
    FormFieldComponent,
    ButtonComponent,
  ],
  templateUrl: './admin-operator-modal.component.html',
})
export class AdminOperatorModalComponent {
  // Servicio con las operaciones sobre operadores en el backend.
  private operatorService = inject(OperatorService);
  // Muestra avisos temporales de éxito.
  private toast = inject(ToastService);

  // Operador a editar; null indica que el modal se usa para crear uno nuevo.
  @Input() operator: Operator | null = null;

  // Avisa al padre que debe cerrar el modal.
  @Output() closed = new EventEmitter<void>();
  // Avisa al padre que el operador se guardó correctamente.
  @Output() saved = new EventEmitter<void>();
  // Le avisa al padre que cierre este modal y abra el de contraseña
  @Output() changePassword = new EventEmitter<void>();

  // Campos del formulario; el estado activo es true por defecto.
  name = '';
  lastName = '';
  username = '';
  email = '';
  password = '';
  active = true;

  // Mensaje de validación o de error del servidor que se muestra en el formulario.
  errorMessage = '';

  // Cada vez que cambia el operador recibido, rellena el formulario con sus datos (edición) o lo deja vacío (creación).
  ngOnChanges(changes: SimpleChanges) {
    // Ignora los cambios que no corresponden al operador.
    if (!changes['operator']) {
      return;
    }

    // Limpia los errores de un uso anterior del modal.
    this.errorMessage = '';

    if (this.operator) {
      this.name = this.operator.name;
      this.lastName = this.operator.lastName;
      this.username = this.operator.username;
      this.email = this.operator.email;
      this.active = this.operator.active;
      // el back no devuelve la contraseña, por lo que no podemos mostrarla en el formulario
      this.password = '';
    } else {
      // Modo creación: formulario vacío y operador activo por defecto.
      this.name = '';
      this.lastName = '';
      this.username = '';
      this.email = '';
      this.active = true;
      this.password = '';
    }
  }
  // Los siguientes métodos actualizan cada campo del formulario con el valor escrito o seleccionado por el usuario.
  setName(event: Event) {
    this.name = (event.target as HTMLInputElement).value;
  }
  setLastName(event: Event) {
    this.lastName = (event.target as HTMLInputElement).value;
  }
  setUsername(event: Event) {
    this.username = (event.target as HTMLInputElement).value;
  }
  setEmail(event: Event) {
    this.email = (event.target as HTMLInputElement).value;
  }
  setPassword(event: Event) {
    this.password = (event.target as HTMLInputElement).value;
  }
  // El select entrega texto (true o false), por eso se convierte a booleano.
  setActive(event: Event) {
    this.active = (event.target as HTMLSelectElement).value === 'true';
  }

  // Valida los campos antes de guardar; si algo falla deja el mensaje en errorMessage y devuelve false.
  private validateFields(): boolean {
    // Nombre, apellido, usuario y correo son obligatorios.
    if (
      !this.name.trim() ||
      !this.lastName.trim() ||
      !this.username.trim() ||
      !this.email.trim()
    ) {
      this.errorMessage =
        'Nombre, apellido, nombre de usuario y correo electrónico son obligatorios.';
      return false;
    }
    // Comprueba el formato del correo electrónico.
    if (!isValidEmail(this.email)) {
      this.errorMessage = EMAIL_ERROR_MESSAGE;
      return false;
    }
    // La contraseña solo es obligatoria al crear; al editar se cambia desde su propio modal.
    if (!this.operator && !this.password.trim()) {
      this.errorMessage =
        'La contraseña es obligatoria para crear un nuevo operador.';
      return false;
    }
    return true;
  }

  // Valida y guarda: actualiza si hay un operador y crea uno nuevo en caso contrario.
  save() {
    this.errorMessage = '';

    // Detiene el guardado si el formulario no es válido.
    if (!this.validateFields()) {
      return;
    }

    //EDITAR Operador
    if (this.operator) {
      // Conserva los datos originales (id, rol, etc.) y sobrescribe los campos editados sin espacios sobrantes.
      const updated: Operator = {
        ...this.operator,
        name: this.name.trim(),
        lastName: this.lastName.trim(),
        username: this.username.trim(),
        email: this.email.trim(),
        active: this.active,
      };

      // Envía los cambios al backend.
      this.operatorService.updateOperator(updated).subscribe({
        next: () =>
          this.onSuccess('Cambios del operador guardados correctamente'),
        error: (error) => this.onError(error),
      });

      // Termina aquí para no ejecutar la creación.
      return;
    }

    // CREAR (no se manda id porque el back lo genera automáticamente)
    const created: CreateOperator = {
      name: this.name.trim(),
      lastName: this.lastName.trim(),
      username: this.username.trim(),
      email: this.email.trim(),
      password: this.password,
      role: Role.OPERATOR, // Asignar el rol de operador al crear un nuevo operador
      active: this.active,
    };

    this.operatorService.addOperator(created).subscribe({
      next: () => this.onSuccess('Operador creado correctamente'),
      error: (error) => this.onError(error),
    });
  }

  // Tras guardar: actualiza la lista de operadores, muestra el aviso y cierra el modal.
  private onSuccess(message: string) {
    this.operatorService.refresh();
    this.toast.show(message);
    this.saved.emit();
    this.closed.emit();
  }

  // Muestra en el formulario el mensaje de error del servidor o uno genérico.
  private onError(error: any) {
    console.error('Error guardando operador:', error);
    this.errorMessage = getErrorMessage(
      error,
      'No se pudo guardar el operador.',
    );
  }

  // Cierra el modal sin guardar.
  close() {
    this.closed.emit();
  }
}
