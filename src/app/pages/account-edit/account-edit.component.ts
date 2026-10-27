import { Component, OnInit, inject } from '@angular/core';
import { ActivatedRoute, Router } from '@angular/router';
import { FormControl, FormGroup } from '@angular/forms';
import { AuthService } from '../../services/auth.service';
import { Client } from '../../models/client';
import { NavbarComponent } from '../../components/navbar/navbar.component';
import { FooterComponent } from '../../components/footer/footer.component';
import { ClientService } from '../../services/client.service';
import { PageHeaderComponent } from '../../components/page-header/page-header.component';
import { ProfileSummaryComponent } from './components/profile-summary/profile-summary.component';
import { AccountFormComponent } from './components/account-form/account-form.component';
import { ChangePasswordModalComponent } from '../../components/change-password-modal/change-password-modal.component';
import { ToastService } from '../../services/toast.service';
import { EMAIL_ERROR_MESSAGE, isValidEmail } from '../../utils/email';
import { getErrorMessage } from '../../utils/http-error';
@Component({
  selector: 'app-account-edit',
  // Componentes utilizados en la vista para mostrar y editar el perfil
  imports: [
    NavbarComponent,
    FooterComponent,
    PageHeaderComponent,
    ProfileSummaryComponent,
    AccountFormComponent,
    ChangePasswordModalComponent,
  ],
  templateUrl: './account-edit.component.html',
})
export class AccountEditComponent implements OnInit {
  // Servicios utilizados para consultar y actualizar los datos del cliente
  private clientService = inject(ClientService);
  private route = inject(ActivatedRoute);
  private router = inject(Router);
  private auth = inject(AuthService);
  private toast = inject(ToastService);

  // Almacena los datos del cliente que se está editando
  client: Client | null = null;

  // Formulario reactivo con los campos editables del perfil
  form = new FormGroup({
    name: new FormControl(''),
    lastName: new FormControl(''),
    email: new FormControl(''),
    phone: new FormControl(''),
    address: new FormControl(''),
  });

  // Controla la apertura del modal de cambio de contraseña y los mensajes de error
  passwordModalOpen = false;
  errorMessage = '';

  // Obtiene el cliente por su ID y carga sus datos en el formulario
  ngOnInit(): void {
    const id = Number(this.route.snapshot.paramMap.get('id'));

    this.clientService.getClientById(id).subscribe({
      next: (client) => {
        this.client = client;

        // Rellena el formulario con la información actual del cliente
        this.form.patchValue({
          name: client.name,
          lastName: client.lastName,
          email: client.email,
          phone: client.phone,
          address: client.address,
        });
      },
      error: (error) => {
        console.error('Error cargando cliente:', error);
        // Si no se pueden cargar los datos, vuelve a la cuenta del cliente
        this.router.navigate(['/account', id]);
      },
    });
  }

  // Valida los datos del formulario y guarda los cambios del perfil
  save(): void {
    if (!this.client) {
      return;
    }

    const email = (this.form.value.email ?? '').trim();

    // Comprueba que el correo ingresado tenga un formato válido
    if (!isValidEmail(email)) {
      this.errorMessage = EMAIL_ERROR_MESSAGE;
      return;
    }

    this.errorMessage = '';

    // Construye el objeto con los datos actualizados del cliente
    const updatedClient: Client = {
      ...this.client,

      name: this.form.value.name ?? '',
      lastName: this.form.value.lastName ?? '',
      email,
      phone: this.form.value.phone ?? '',
      password: '', // La contraseña no se modifica desde este formulario
      address: this.form.value.address ?? '', // Incluye la dirección en los datos actualizados
    };

    // Envía los cambios al servidor y actualiza la sesión con la respuesta
    this.clientService.updateClient(updatedClient).subscribe({
      next: (savedClient) => {
        this.auth.setSession(savedClient);
        this.toast.show('Cambios guardados correctamente');
        // Redirige al perfil después de guardar los cambios
        this.router.navigate(['/account', this.client!.id]);
      },
      error: (error) => {
        console.error('Error actualizando cliente:', error);
        // Muestra un mensaje si ocurre un problema al guardar los cambios
        this.errorMessage = getErrorMessage(
          error,
          'No se pudieron guardar los cambios. Inténtalo de nuevo.',
        );
      },
    });
  }

  // Cancela la edición y regresa al perfil del cliente
  cancel(): void {
    if (!this.client) {
      return;
    }

    this.router.navigate(['/account', this.client.id]);
  }

  // Solicita al servicio el cambio de contraseña del cliente
  savePassword = (current: string, next: string) =>
    this.clientService.changePassword(this.client!.id, current, next);
}
