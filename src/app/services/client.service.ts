import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';

import { Client } from '../models/client';
import { Role } from '../models/role';

@Injectable({
  providedIn: 'root',
})
export class ClientService {
  private apiUrl = 'http://localhost:8080/clients';

  constructor(private http: HttpClient) {}

  // =========================
  // CRUD conectado a Spring Boot
  // =========================

  getClients(): Observable<Client[]> {
    return this.http.get<Client[]>(this.apiUrl);
  }

  getClientById(id: number): Observable<Client> {
    return this.http.get<Client>(`${this.apiUrl}/${id}`);
  }

  addClient(client: Client): Observable<Client> {
    return this.http.post<Client>(this.apiUrl, client);
  }

  updateClient(client: Client): Observable<Client> {
    return this.http.put<Client>(`${this.apiUrl}/${client.id}`, client);
  }

  deleteClient(id: number): Observable<void> {
    return this.http.delete<void>(`${this.apiUrl}/${id}`);
  }

  // =========================
  // Mini login local
  // =========================

  login(email: string, password: string): Client | undefined {
    const demoClient: Client = {
      id: 1,
      name: 'Juan',
      lastName: 'García',
      email: 'cliente@terra.com',
      password: 'cliente123',
      phone: '+57 300 000 0000',
      role: Role.CLIENT,
    };

    if (demoClient.email === email && demoClient.password === password) {
      return demoClient;
    }

    return undefined;
  }
}
