import { Component, inject, OnInit } from '@angular/core';
import { Router, ActivatedRoute } from '@angular/router';

import { NavbarComponent } from '../../components/navbar/navbar.component';
import { FooterComponent } from '../../components/footer/footer.component';
import { ButtonComponent } from '../../components/button/button.component';

import { Client } from '../../models/client';
import { ClientService } from '../../services/client.service';

@Component({
  selector: 'app-account',
  imports: [NavbarComponent, FooterComponent, ButtonComponent],
  templateUrl: './account.component.html',
})
export class AccountComponent implements OnInit {
  private clientService = inject(ClientService);
  private route = inject(ActivatedRoute);
  private router = inject(Router);

  client: Client | null = null;

  ngOnInit() {
    const id = Number(this.route.snapshot.paramMap.get('id'));

    this.clientService.getClientById(id).subscribe({
      next: (client) => {
        this.client = client;
      },
      error: (error) => {
        console.error('Error cargando cliente:', error);
        this.client = null;
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
    localStorage.removeItem('currentUser');

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
        localStorage.removeItem('currentUser');
        this.router.navigate(['/home']);
      },
      error: (error) => {
        console.error('Error eliminando cuenta:', error);
      },
    });
  }
}
