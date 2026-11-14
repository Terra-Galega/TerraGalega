import { Component, inject, OnInit } from '@angular/core';
import { Router } from '@angular/router';
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
import { ModalComponent } from '../../components/modal/modal.component';
import { ToastService } from '../../services/toast.service';
import { getErrorMessage } from '../../utils/http-error';
import { AddressListComponent } from './components/address-list/address-list.component';

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
    AccountActionsComponent,
    AddressListComponent,
    ButtonComponent,
  ],
  templateUrl: './account.component.html',
})
export class AccountComponent implements OnInit {
  //Injextar los servicios que se van a usar
  private clientService = inject(ClientService);
  private router = inject(Router);
  private auth = inject(AuthService);
  private toast = inject(ToastService);

  client: Client | null = null;
  //variables para mostrar u ocultar 
  showDeleteModal = false;
  showErrorModal = false;
  confirmTitle = 'Eliminar cuenta';
  confirmMessage =
    '¿Seguro que quieres eliminar tu cuenta? Esta acción no se puede deshacer.';
  errorTitle = '';
  errorMessage = '';

  ngOnInit() {
    //si no ha iniciado sesión
    if (!this.auth.session) {
      this.router.navigate(['/login']);
      return;
    }
    //traer el cliente (el back lo identifica por el token)
    this.clientService.getMe().subscribe({
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
  //editar perfil
  editProfile() {
    if (!this.client) {
      return;
    }

    this.router.navigate(['/account', 'edit']);
  }
  //cerrar sesion
  logout() {
    this.auth.logout();
    this.router.navigate(['/login']);
  }
 //borrar cuenta
  deleteAccount() {
    if (!this.client) {
      return;
    }

    this.showDeleteModal = true;
  }
  //btn de cancelar 
  cancelDelete() {
    this.showDeleteModal = false;
  }
  //confirmar cancelar 
  confirmDelete() {
    if (!this.client) {
      return;
    }

    this.showDeleteModal = false;
    // MANDAR AL SERVICE -> BACKK  a  borrar 
    this.clientService.deleteMe().subscribe({
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