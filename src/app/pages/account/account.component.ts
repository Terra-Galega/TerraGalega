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

@Component({
  selector: 'app-account',
  imports: [
    NavbarComponent,
    FooterComponent,
    PageHeaderComponent,
    ProfileCardComponent,
    PersonalInfoComponent,
    AccountActionsComponent,
    ButtonComponent
  ],
  templateUrl: './account.component.html',
})
export class AccountComponent implements OnInit {
  private clientService = inject(ClientService);
  private route = inject(ActivatedRoute);
  private router = inject(Router);

  client: Client | null = null;

  private auth = inject(AuthService);

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
  selectedAction = '';
  onSelectedChange(action: string) {
    this.selectedAction = action;

    switch (action) {
      case 'Editar perfil':
        this.editProfile();
        break;

      case 'Cerrar sesion':
        this.logout();
        break;

      case 'Eliminar cuenta':
        this.deleteAccount();
        break;
    }
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

    const confirmed = confirm(
      '¿Seguro que querés eliminar tu cuenta? Esta acción no se puede deshacer.',
    );

    if (!confirmed) {
      return;
    }

    this.clientService.deleteClient(this.client.id).subscribe({
      next: () => {
        this.auth.logout();
        this.router.navigate(['/home']);
      },
      error: (error) => {
        console.error('Error eliminando cuenta:', error);
      },
    });
  }
}
