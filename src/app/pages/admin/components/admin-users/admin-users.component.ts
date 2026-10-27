import { Component, inject, Input, OnChanges } from '@angular/core';
import { ClientService } from '../../../../services/client.service';
import { AdminService } from '../../../../services/admin.service';
import { Client, ClientDTO } from '../../../../models/client';
import { filter } from 'rxjs';

@Component({
  selector: 'app-admin-users',
  imports: [],
  templateUrl: './admin-users.component.html',
  styleUrl: './admin-users.component.scss',
})
export class AdminUsersComponent implements OnChanges {
  private clientService = inject(ClientService);
  private adminService = inject(AdminService);
  @Input() searchTerm = '';

  clients: ClientDTO[] = [];
  filteredClients: ClientDTO[] = [];

  ngOnChanges() {
    this.filterClients();
  }

  filterClients() {
    const value = this.searchTerm.trim().toLowerCase();
    this.filteredClients = this.clients.filter((client) =>
      `${client.name} ${client.email} ${client.phone ?? ''}`
        .toLowerCase()
        .includes(value),
    );
  }

  constructor() {
    this.loadClients();

    this.clientService.refresh$.subscribe(() => {
      this.loadClients();
    });
  }

  loadClients() {
    this.adminService.getClientDTOAdmins().subscribe({
      next: (clientsD) => {
        this.clients = clientsD;
        this.filterClients();
      },
      error: (error) => {
        console.error('Error cargando clientes:', error);
      },
    });
  }
}
