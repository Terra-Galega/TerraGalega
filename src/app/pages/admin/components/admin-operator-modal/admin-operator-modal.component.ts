import {
  Component,
  EventEmitter,
  inject,
  Input,
  Output,
  SimpleChange,
  SimpleChanges,
} from '@angular/core';
import { FormFieldComponent } from '../../../../components/form-field/form-field.component';
import { IconButtonComponent } from '../../../../components/icon-button/icon-button.component';
import { OperatorService } from '../../../../services/operator.service';
import { CreateOperator, Operator, Role } from '../../../../models/operator';
import { ButtonComponent } from '../../../../components/button/button.component';

@Component({
  selector: 'app-admin-operator-modal',
  imports: [IconButtonComponent, IconButtonComponent, FormFieldComponent, ButtonComponent],
  templateUrl: './admin-operator-modal.component.html',
})
export class AdminOperatorModalComponent {
  private operatorService = inject(OperatorService);

  @Input() operator: Operator | null = null;

  @Output() closed = new EventEmitter<void>();
  @Output() saved = new EventEmitter<void>();

  name = '';
  lastName = '';
  username = '';
  email = '';
  password = '';
  active = true;

  errorMessage = '';

  ngOnChanges(changes: SimpleChanges) {
    if (!changes['operator']) {
      return;
    }

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
      this.name = '';
      this.lastName = '';
      this.username = '';
      this.email = '';
      this.active = true;
      this.password = '';
    }
  }
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
  setActive(event: Event) {
    this.active = (event.target as HTMLInputElement).checked;
  }

  private validateFields(): boolean {
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
    if (!this.email.includes('@')) {
      this.errorMessage = 'El correo electrónico no es válido.';
      return false;
    }
    if (!this.operator && !this.password.trim()) {
      this.errorMessage =
        'La contraseña es obligatoria para crear un nuevo operador.';
      return false;
    }
    return true;
  }

  save() {
    this.errorMessage = '';

    if (!this.validateFields()) {
      return;
    }

    //EDITAR Operador
    if (this.operator) {
      const updated: Operator = {
        ...this.operator,
        name: this.name.trim(),
        lastName: this.lastName.trim(),
        username: this.username.trim(),
        email: this.email.trim(),
        active: this.active,
        // No se envía la contraseña si no se ha modificado
        password: this.password,
      };

      this.operatorService.updateOperator(updated).subscribe({
        next: () => this.onSuccess(),
        error: (error) => this.onError(error),
      });

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
      next: () => this.onSuccess(),
      error: (error) => this.onError(error),
    });
  }
  private onSuccess() {
    this.operatorService.refresh();
    this.saved.emit();
    this.closed.emit();
  }

  private onError(error: any) {
    console.error('Error guardando operador:', error);
    this.errorMessage =
      error.error?.message ?? 'No se pudo guardar el operador.';
  }

  close() {
    this.closed.emit();
  }
}
