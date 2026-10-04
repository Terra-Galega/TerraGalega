import { Injectable } from '@angular/core';
import { Client } from '../models/client';

@Injectable({
  providedIn: 'root',
})
export class ClientService {
  private clients: Client[] = [
    {
      id: 1,
      name: 'Juan',
      lastName: 'García',
      email: 'cliente@terra.com',
      password: 'cliente123',
      phone: '+57 300 000 0000',
    },
    {
      id: 2,
      name: 'Marta',
      lastName: 'Souto',
      email: 'marta.souto@terra.com',
      password: 'marta123',
      phone: '+57 310 555 1122',
    },
    {
      id: 3,
      name: 'Diego',
      lastName: 'Pardo',
      email: 'diego.pardo@terra.com',
      password: 'diego123',
      phone: '+57 320 444 9988',
    },
    {
      id: 4,
      name: 'Lucía',
      lastName: 'Fernández',
      email: 'laura.fernandez@terra.com',
      password: 'laura123',
      phone: '+57 311 666 7788',
    },

    {
      id: 5,
      name: 'Andrés',
      lastName: 'Martínez',
      email: 'andres.martinez@terra.com',
      password: 'andres123',
      phone: '+57 301 222 3344',
    },
    {
      id: 6,
      name: 'Laura',
      lastName: 'Ramírez',
      email: 'laura.ramirez@terra.com',
      password: 'laura123',
      phone: '+57 311 666 7788',
    },
    {
      id: 7,
      name: 'Sebastián',
      lastName: 'Torres',
      email: 'sebastian.torres@terra.com',
      password: 'sebastian123',
      phone: '+57 322 111 2233',
    },
    {
      id: 8,
      name: 'Camila',
      lastName: 'Moreno',
      email: 'camila.moreno@terra.com',
      password: 'camila123',
      phone: '+57 300 888 4455',
    },
    {
      id: 9,
      name: 'Felipe',
      lastName: 'Castro',
      email: 'felipe.castro@terra.com',
      password: 'felipe123',
      phone: '+57 316 333 5566',
    },
  ];

  constructor() {}


  login(email: string, password: string): Client | null {
    const client = this.clients.find(
      (c) => c.email === email && c.password === password
    );
    return client || null;
  }

  getClients(): Client[] {
    return this.clients;
  }

  updateClient(updatedClient: Client): void {
    const index = this.clients.findIndex((c) => c.id === updatedClient.id);
    if (index !== -1) {
      this.clients[index] = updatedClient;
    }
  }

  deleteClient(clientId: number): void {
    this.clients = this.clients.filter((c) => c.id !== clientId);
  }


  addClient(newClient: Client): void {
    const newId = this.clients.length > 0 ? Math.max(...this.clients.map(c => c.id)) + 1 : 1;
    newClient.id = newId;
    this.clients.push(newClient);
  }

}
