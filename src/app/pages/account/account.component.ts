import { Component, inject, OnInit } from '@angular/core';
import { Router, ActivatedRoute } from '@angular/router';
import { NavbarComponent } from '../../components/navbar/navbar.component';
import { FooterComponent } from '../../components/footer/footer.component';
import { PageHeaderComponent } from '../../components/page-header/page-header.component';
import { ProfileCardComponent } from './components/profile-card/profile-card.component';
import { PersonalInfoComponent } from './components/personal-info/personal-info.component';
import { AccountActionsComponent } from './components/account-actions/account-actions.component';
import { AuthService } from '../../services/auth.service';
import { Client } from '../../models/client';
import { ClientService } from '../../services/client.service';
import { ButtonComponent } from '../../components/button/button.component';
import {
  ModalComponent,
  getErrorMessage,
} from '../../components/modal/modal.component';
import { ToastService } from '../../services/toast.service';

@Component({
  selector: 'app-account',
  imports: [
    NavbarComponent,
    FooterComponent,
    PageHeaderComponent,
    ProfileCardComponent,
    PersonalInfoComponent,
    AccountActionsComponent,
    ButtonComponent,
    ModalComponent,
  ],
  templateUrl: './account.component.html',
})
export class AccountComponent implements OnInit {
  private clientService = inject(ClientService);
  private route = inject(ActivatedRoute);
  private router = inject(Router);
  private auth = inject(AuthService);
  private toast = inject(ToastService);

  client: Client | null = null;

  showDeleteModal = false;
  showErrorModal = false;
  confirmTitle = 'Eliminar cuenta';
  confirmMessage =
    '¿Seguro que quieres eliminar tu cuenta? Esta acción no se puede deshacer.';
  errorTitle = '';
  errorMessage = '';

  ngOnInit() {
    if (!this.auth.session) {
      this.router.navigate(['/login']);
      return;
    }
    const id = Number(this.route.snapshot.paramMap.get('id'));

    this.clientService.getClientById(id).subscribe({
      next: (client) => {
        this.client = client;
      },
      error: (error) => {
        console.error('Error cargando cliente:', error);
        this.auth.logout();
        this.router.navigate(['/login']);
      },
    });
  }

  editProfile() {
    if (!this.client) {
      return;
    }

    this.router.navigate(['/account', this.client.id, 'edit']);
  }

  logout() {
    this.auth.logout();
    this.router.navigate(['/login']);
  }

  deleteAccount() {
    if (!this.client) {
      return;
    }

    this.showDeleteModal = true;
  }

  cancelDelete() {
    this.showDeleteModal = false;
  }

  confirmDelete() {
    if (!this.client) {
      return;
    }

    this.showDeleteModal = false;

    this.clientService.deleteClient(this.client.id).subscribe({
      next: () => {
        this.auth.logout();
        this.toast.show('Cuenta eliminada correctamente');
        this.router.navigate(['/home']);
      },
      error: (error) => {
        console.error('Error eliminando cuenta:', error);
        this.errorTitle = 'No se pudo eliminar la cuenta';
        this.errorMessage = getErrorMessage(error);
        this.showErrorModal = true;
      },
    });
  }

  closeErrorModal() {
    this.showErrorModal = false;
    this.errorTitle = '';
    this.errorMessage = '';
  }
}
