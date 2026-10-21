import {
  Component,
  EventEmitter,
  Input,
  OnInit,
  Output,
  inject,
} from '@angular/core';
import {
  CreateDeliveryPerson,
  DeliveryPerson,
} from '../../../../models/deliveryPerson';
import { DeliveryPersonService } from '../../../../services/delivery-person.service';
import { ButtonComponent } from '../../../../components/button/button.component';
import { IconButtonComponent } from '../../../../components/icon-button/icon-button.component';
import { FormFieldComponent } from '../../../../components/form-field/form-field.component';
import { CheckOptionComponent } from '../../../../components/check-option/check-option.component';

// Modal para crear o editar un domiciliario.
// Si recibe un domiciliario es edición; si no, es creación.
@Component({
  selector: 'app-admin-delivery-person-modal',
  imports: [
    ButtonComponent,
    IconButtonComponent,
    FormFieldComponent,
    CheckOptionComponent,
  ],
  templateUrl: './admin-delivery-person-modal.component.html',
})
export class AdminDeliveryPersonModalComponent implements OnInit {
  private deliveryPersonService = inject(DeliveryPersonService);

  @Input() deliveryPerson: DeliveryPerson | null = null;

  @Output() closed = new EventEmitter<void>();

  // Valores del formulario. La contraseña de un domiciliario existente
  // nunca llega del back, por eso al editar empieza vacía.
  form = {
    name: '',
    lastName: '',
    email: '',
    identification: '',
    phone: '',
    password: '',
    active: true,
  };

  errorMessage = '';

  ngOnInit() {
    if (this.deliveryPerson) {
      this.form = {
        name: this.deliveryPerson.name,
        lastName: this.deliveryPerson.lastName,
        email: this.deliveryPerson.email,
        identification: this.deliveryPerson.identification,
        phone: this.deliveryPerson.phone,
        password: '',
        active: this.deliveryPerson.active,
      };
    }
  }

  toggleActive() {
    this.form.active = !this.form.active;
  }

  save() {
    const { name, lastName, email, identification, phone, password } =
      this.form;

    if (
      !name.trim() ||
      !lastName.trim() ||
      !email.trim() ||
      !identification.trim() ||
      !phone.trim()
    ) {
      this.errorMessage = 'Todos los campos son obligatorios.';
      return;
    }

    if (!email.includes('@')) {
      this.errorMessage = 'El correo no es válido.';
      return;
    }

    // La contraseña solo es obligatoria al crear
    if (!this.deliveryPerson && !password.trim()) {
      this.errorMessage = 'La contraseña es obligatoria.';
      return;
    }

    const data = {
      name: name.trim(),
      lastName: lastName.trim(),
      email: email.trim(),
      identification: identification.trim(),
      phone: phone.trim(),
      password,
      active: this.form.active,
    };

    // EDITAR
    if (this.deliveryPerson) {
      const updated: DeliveryPerson = { ...this.deliveryPerson, ...data };

      this.deliveryPersonService.updateDeliveryPerson(updated).subscribe({
        next: () => this.onSuccess(),
        error: (error) => this.onError(error),
      });

      return;
    }

    // CREAR
    const created: CreateDeliveryPerson = data;

    this.deliveryPersonService.addDeliveryPerson(created).subscribe({
      next: () => this.onSuccess(),
      error: (error) => this.onError(error),
    });
  }

  private onSuccess() {
    this.deliveryPersonService.refresh();
    this.closed.emit();
  }

  private onError(error: any) {
    console.error('Error guardando domiciliario:', error);
    this.errorMessage =
      error.error?.message ?? 'No se pudo guardar el domiciliario.';
  }

  close() {
    this.closed.emit();
  }
}
