import { Component, inject } from '@angular/core';
import { ClientService } from '../../../../services/client.service';
import { Client } from '../../../../models/client';

@Component({
  selector: 'app-admin-users',
  imports: [],
  templateUrl: './admin-users.component.html',
  styleUrl: './admin-users.component.scss',
})
export class AdminUsersComponent {
  private clientService = inject(ClientService);

  clients: Client[] = [];

  constructor() {
    this.loadClients();

    this.clientService.refresh$.subscribe(() => {
      this.loadClients();
    });
  }

  loadClients() {
    this.clientService.getClients().subscribe({
      next: (clients) => {
        this.clients = clients;
      },
      error: (error) => {
        console.error('Error cargando clientes:', error);
      },
    });
  }
}
