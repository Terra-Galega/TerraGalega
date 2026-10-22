import { Component, EventEmitter, Input, Output } from '@angular/core';
import { FormGroup, ReactiveFormsModule } from '@angular/forms';
import { ButtonComponent } from '../../../../components/button/button.component';
import { FormFieldComponent } from '../../../../components/form-field/form-field.component';
import { FormSectionComponent } from '../form-section/form-section.component';

// Formulario de edición.
@Component({
  selector: 'app-account-form',
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
  @Input({ required: true }) form!: FormGroup;

  @Output() save = new EventEmitter<void>();
  @Output() cancel = new EventEmitter<void>();
  @Output() changePassword = new EventEmitter<void>();

  showPassword = false;

  togglePassword(): void {
    this.showPassword = !this.showPassword;
  }
}
