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
  private clientService = inject(ClientService);
  private route = inject(ActivatedRoute);
  private router = inject(Router);
  private auth = inject(AuthService);
  private toast = inject(ToastService);

  client: Client | null = null;

  form = new FormGroup({
    name: new FormControl(''),
    lastName: new FormControl(''),
    email: new FormControl(''),
    phone: new FormControl(''),
    address: new FormControl(''),
  });

  passwordModalOpen = false;
  errorMessage = '';

  ngOnInit(): void {
    const id = Number(this.route.snapshot.paramMap.get('id'));

    this.clientService.getClientById(id).subscribe({
      next: (client) => {
        this.client = client;

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
        this.router.navigate(['/account', id]);
      },
    });
  }

  save(): void {
    if (!this.client) {
      return;
    }

    const email = (this.form.value.email ?? '').trim();

    if (!isValidEmail(email)) {
      this.errorMessage = EMAIL_ERROR_MESSAGE;
      return;
    }

    this.errorMessage = '';

    const updatedClient: Client = {
      ...this.client,

      name: this.form.value.name ?? '',
      lastName: this.form.value.lastName ?? '',
      email,
      phone: this.form.value.phone ?? '',
      password: '', // la contraseña no se edita aquí
      address: this.form.value.address ?? '', // se agrega la dirección al objeto actualizado
    };

    this.clientService.updateClient(updatedClient).subscribe({
      next: (savedClient) => {
        this.auth.setSession(savedClient);
        this.toast.show('Cambios guardados correctamente');
        this.router.navigate(['/account', this.client!.id]);
      },
      error: (error) => {
        console.error('Error actualizando cliente:', error);
        this.errorMessage = getErrorMessage(
          error,
          'No se pudieron guardar los cambios. Inténtalo de nuevo.',
        );
      },
    });
  }

  cancel(): void {
    if (!this.client) {
      return;
    }

    this.router.navigate(['/account', this.client.id]);
  }

  savePassword = (current: string, next: string) =>
    this.clientService.changePassword(this.client!.id, current, next);
}
