import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable, Subject } from 'rxjs';
import { Client, CreateClient } from '../models/client';
import { Role } from '../models/role';

// Servicio encargado de gestionar los clientes y comunicarse con el backend.
@Injectable({
  providedIn: 'root',
})
export class ClientService {
  // URL base de los endpoints de clientes en Spring Boot.
  private apiUrl = 'http://localhost:8080/clients';

  // =========================
  // Actualización de clientes
  // =========================

  // Notifica a los componentes cuando necesitan actualizar la información de clientes.
  private refreshSubject = new Subject<void>();

  // Observable al que se pueden suscribir los componentes para recibir estas notificaciones.
  refresh$ = this.refreshSubject.asObservable();

  // Emite una notificación para solicitar la actualización de los datos.
  refresh() {
    this.refreshSubject.next();
  }

  // Inyecta el cliente HTTP para realizar solicitudes al backend.
  constructor(private http: HttpClient) {}

  // =========================
  // CRUD conectado a Spring Boot
  // =========================

  // Obtiene la lista completa de clientes.
  getClients(): Observable<Client[]> {
    return this.http.get<Client[]>(this.apiUrl);
  }

  // Busca un cliente por su identificador.
  getClientById(id: number): Observable<Client> {
    return this.http.get<Client>(`${this.apiUrl}/${id}`);
  }

  // Registra un nuevo cliente en el backend.
  addClient(client: CreateClient): Observable<Client> {
    return this.http.post<Client>(this.apiUrl, client);
  }

  // Actualiza los datos de un cliente existente.
  updateClient(client: Client): Observable<Client> {
    return this.http.put<Client>(`${this.apiUrl}/${client.id}`, client);
  }

  // Elimina un cliente según su identificador.
  deleteClient(id: number): Observable<void> {
    return this.http.delete<void>(`${this.apiUrl}/${id}`);
  }

  // Solicita al backend el cambio de contraseña de un cliente.
  // Envía la contraseña actual y la nueva contraseña.
  changePassword(
    id: number,
    currentPassword: string,
    newPassword: string,
  ): Observable<void> {
    return this.http.put<void>(`${this.apiUrl}/${id}/password`, {
      currentPassword,
      newPassword,
    });
  }

  // =========================
  // Mini login local
  // =========================

  // Valida las credenciales de un cliente de prueba almacenado localmente.
  login(email: string, password: string): Client | undefined {
    // Datos de demostración utilizados para el inicio de sesión local.
    const demoClient: Client = {
      id: 1,
      name: 'Juan',
      lastName: 'García',
      email: 'cliente@terra.com',
      password: 'cliente123',
      phone: '+57 300 000 0000',
      role: Role.CLIENT,
      address: 'Calle Falsa 123, Ciudad, País',
    };

    // Devuelve el cliente si las credenciales coinciden.
    if (demoClient.email === email && demoClient.password === password) {
      return demoClient;
    }

    // Si las credenciales no coinciden, indica que no se encontró el cliente.
    return undefined;
  }
}
