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
import { ToastService } from '../../../../services/toast.service';
import { getErrorMessage } from '../../../../utils/http-error';
import { EMAIL_ERROR_MESSAGE, isValidEmail } from '../../../../utils/email';

// Modal del admin para crear o editar un domiciliario.
// El mismo componente sirve para los dos casos: si llega `deliveryPerson`
// es edición; si es null, es creación.
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
  private toast = inject(ToastService);

  // Domiciliario a editar. null = modo "Añadir domiciliario"
  @Input() deliveryPerson: DeliveryPerson | null = null;

  // Avisa al padre que debe cerrar el modal (cancelar o guardado exitoso)
  @Output() closed = new EventEmitter<void>();
  // Avisa que se guardó correctamente
  @Output() saved = new EventEmitter<void>();
  // Le avisa al padre que cierre este modal y abra el de contraseña
  @Output() changePassword = new EventEmitter<void>();

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

  // Mensaje de validación o de error del servidor que se muestra bajo el formulario
  errorMessage = '';

  // Al abrir en modo edición, precarga el formulario con los datos actuales
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

  // Invierte el checkbox "Activo"
  toggleActive() {
    this.form.active = !this.form.active;
  }

  // Valida el formulario y decide si crear o actualizar
  save() {
    const { name, lastName, email, identification, phone, password } =
      this.form;

    // Validación 1: ningún campo de texto puede estar vacío (o solo espacios)
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

    // Validación 2: formato de correo válido
    if (!isValidEmail(email)) {
      this.errorMessage = EMAIL_ERROR_MESSAGE;
      return;
    }

    // La contraseña solo es obligatoria al crear
    if (!this.deliveryPerson && !password.trim()) {
      this.errorMessage = 'La contraseña es obligatoria.';
      return;
    }

    // Datos limpios (sin espacios sobrantes) listos para enviar al backend.
    // La contraseña se manda tal cual, sin trim, para no alterarla.
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
      // Se mezcla el domiciliario original con los datos nuevos para conservar
      // id, rol y disponibilidad, que el formulario no maneja
      const updated: DeliveryPerson = { ...this.deliveryPerson, ...data };

      this.deliveryPersonService.updateDeliveryPerson(updated).subscribe({
        next: () =>
          this.onSuccess('Cambios del domiciliario guardados correctamente'),
        error: (error) => this.onError(error),
      });

      return;
    }

    // CREAR
    // CreateDeliveryPerson no lleva id, rol ni disponibilidad: los define el back
    const created: CreateDeliveryPerson = data;

    this.deliveryPersonService.addDeliveryPerson(created).subscribe({
      next: () => this.onSuccess('Domiciliario creado correctamente'),
      error: (error) => this.onError(error),
    });
  }

  // Éxito (crear o editar): refresca la tabla, muestra el aviso y cierra el modal
  private onSuccess(message: string) {
    this.deliveryPersonService.refresh();
    this.toast.show(message);
    this.closed.emit();
  }

  // Error del servidor: lo muestra dentro del modal (no lo cierra)
  // para que el admin pueda corregir y reintentar
  private onError(error: any) {
    console.error('Error guardando domiciliario:', error);
    this.errorMessage = getErrorMessage(
      error,
      'No se pudo guardar el domiciliario.',
    );
  }

  // Cancelar / cerrar con la X
  close() {
    this.closed.emit();
  }
}
