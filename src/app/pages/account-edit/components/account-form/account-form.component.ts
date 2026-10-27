import { Component, EventEmitter, Input, Output } from '@angular/core';
import { FormGroup, ReactiveFormsModule } from '@angular/forms';
import { ButtonComponent } from '../../../../components/button/button.component';
import { FormFieldComponent } from '../../../../components/form-field/form-field.component';
import { FormSectionComponent } from '../form-section/form-section.component';

// Formulario reutilizable para editar los datos de la cuenta del cliente.
@Component({
  selector: 'app-account-form',
  // Componentes y módulos necesarios para construir el formulario
  imports: [
    ReactiveFormsModule,
    ButtonComponent,
    FormFieldComponent,
    FormSectionComponent,
  ],
  templateUrl: './account-form.component.html',
  host: { class: 'block' },
})
export class AccountFormComponent {
  // Formulario reactivo recibido del componente padre
  @Input({ required: true }) form!: FormGroup;

  // Mensaje de error que se muestra cuando ocurre un problema al guardar
  @Input() errorMessage = '';

  // Eventos que notifican al componente padre las acciones del usuario
  @Output() save = new EventEmitter<void>();
  @Output() cancel = new EventEmitter<void>();
  @Output() changePassword = new EventEmitter<void>();

  // Controla la visibilidad de la contraseña
  showPassword = false;

  // Alterna la visibilidad de la contraseña
  togglePassword(): void {
    this.showPassword = !this.showPassword;
  }
}
