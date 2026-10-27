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
  // Servicios para obtener los clientes y detectar cuándo se debe actualizar la lista.
  private clientService = inject(ClientService);
  private adminService = inject(AdminService);

  // Término de búsqueda recibido desde el componente padre.
  @Input() searchTerm = '';

  // Listas de clientes: una contiene todos los registros y la otra los resultados filtrados.
  clients: ClientDTO[] = [];
  filteredClients: ClientDTO[] = [];

  // Actualiza los resultados cuando cambia el término de búsqueda.
  ngOnChanges() {
    this.filterClients();
  }

  // Filtra los clientes por nombre, correo electrónico o teléfono.
  filterClients() {
    const value = this.searchTerm.trim().toLowerCase();
    this.filteredClients = this.clients.filter((client) =>
      `${client.name} ${client.email} ${client.phone ?? ''}`
        .toLowerCase()
        .includes(value),
    );
  }

  constructor() {
    // Carga los clientes al inicializar el componente.
    this.loadClients();

    // Recarga la lista cuando el servicio emite una solicitud de actualización.
    this.clientService.refresh$.subscribe(() => {
      this.loadClients();
    });
  }

  // Consulta al servicio administrativo y actualiza las listas de clientes.
  loadClients() {
    this.adminService.getClientDTOAdmins().subscribe({
      next: (clientsD) => {
        this.clients = clientsD;
        this.filterClients();
      },
      error: (error) => {
        // Registra el error si no es posible cargar los clientes.
        console.error('Error cargando clientes:', error);
      },
    });
  }
}
