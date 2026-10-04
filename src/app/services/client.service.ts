import { Injectable } from '@angular/core';
import { Client } from '../models/client';
import { Role } from '../models/role';

@Injectable({
  providedIn: 'root',
})
export class ClientService {
  role : Role = Role.CLIENT;
  private clients: Client[] = [
    {
      id: 1,
      name: 'Juan',
      lastName: 'García',
      email: 'cliente@terra.com',
      password: 'cliente123',
      phone: '+57 300 000 0000',
      role: this.role,
    },
    {
      id: 2,
      name: 'Marta',
      lastName: 'Souto',
      email: 'marta.souto@terra.com',
      password: 'marta123',
      phone: '+57 310 555 1122',
      role: this.role,
    },
    {
      id: 3,
      name: 'Diego',
      lastName: 'Pardo',
      email: 'diego.pardo@terra.com',
      password: 'diego123',
      phone: '+57 320 444 9988',
      role: this.role,
    },
    {
      id: 4,
      name: 'Lucía',
      lastName: 'Fernández',
      email: 'laura.fernandez@terra.com',
      password: 'laura123',
      phone: '+57 311 666 7788',
      role: this.role,
    },

    {
      id: 5,
      name: 'Andrés',
      lastName: 'Martínez',
      email: 'andres.martinez@terra.com',
      password: 'andres123',
      phone: '+57 301 222 3344',
      role: this.role,
    },
    {
      id: 6,
      name: 'Laura',
      lastName: 'Ramírez',
      email: 'laura.ramirez@terra.com',
      password: 'laura123',
      phone: '+57 311 666 7788',
      role: this.role,
    },
    {
      id: 7,
      name: 'Sebastián',
      lastName: 'Torres',
      email: 'sebastian.torres@terra.com',
      password: 'sebastian123',
      phone: '+57 322 111 2233',
      role: this.role,
    },
    {
      id: 8,
      name: 'Camila',
      lastName: 'Moreno',
      email: 'camila.moreno@terra.com',
      password: 'camila123',
      phone: '+57 300 888 4455',
      role: this.role,
    },
    {
      id: 9,
      name: 'Felipe',
      lastName: 'Castro',
      email: 'felipe.castro@terra.com',
      password: 'felipe123',
      phone: '+57 316 333 5566',
      role: this.role,
    },
  ];

  constructor() {}

  getClients(): Client[] {
    return this.clients;
  }

  getClientById(id: number): Client | undefined {
    return this.clients.find((client) => client.id === id);
  }

  addClient(client: Client): void {
    const newId =
      this.clients.length > 0
        ? Math.max(...this.clients.map((c) => c.id)) + 1
        : 1;

    client.id = newId;

    this.clients.push(client);
  }

  updateClient(updatedClient: Client): void {
    const index = this.clients.findIndex(
      (client) => client.id === updatedClient.id,
    );

    if (index === -1) {
      return;
    }

    this.clients[index] = updatedClient;
  }

  deleteClient(id: number): void {
    this.clients = this.clients.filter((client) => client.id !== id);
  }

  login(email: string, password: string): Client | undefined {
    return this.clients.find(
      (client) => client.email === email && client.password === password,
    );
  }
}
