import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable, Subject } from 'rxjs';
import { Client } from '../models/client';
import { API_URL, LoginResponse } from './auth.service';

// Servicio de clientes. Lo que hace el cliente sobre su propia cuenta va por
// /clients/me: el back sabe quién es por el token, así que no se envía ningún id.
@Injectable({
  providedIn: 'root',
})
export class ClientService {
  private apiUrl = `${API_URL}/clients`;

  // Avisa a los componentes cuando deben recargar la lista de clientes.
  private refreshSubject = new Subject<void>();
  refresh$ = this.refreshSubject.asObservable();

  refresh() {
    this.refreshSubject.next();
  }

  constructor(private http: HttpClient) {}

  // =========================
  // Admin
  // =========================

  // Lista completa de clientes (la usa el modal de cupones del admin).
  getClients(): Observable<Client[]> {
    return this.http.get<Client[]>(this.apiUrl);
  }

  // =========================
  // Cliente con sesión
  // =========================

  // Datos del cliente con sesión iniciada.
  getMe(): Observable<Client> {
    return this.http.get<Client>(`${this.apiUrl}/me`);
  }

  // Actualiza su perfil. La respuesta trae un token nuevo por si cambió el correo.
  updateMe(client: Client): Observable<LoginResponse> {
    return this.http.put<LoginResponse>(`${this.apiUrl}/me`, client);
  }

  // Cuenta creada con Google: guarda lo que Google no da (apellido, teléfono y
  // la primera dirección). Devuelve la sesión con profileComplete actualizado.
  completeProfile(data: {
    name: string;
    lastName: string;
    phone: string;
    address?: { label: string; name?: string };
  }): Observable<LoginResponse> {
    return this.http.put<LoginResponse>(`${this.apiUrl}/me/complete-profile`, data);
  }

  // Elimina su cuenta.
  deleteMe(): Observable<void> {
    return this.http.delete<void>(`${this.apiUrl}/me`);
  }

  // Cambia su contraseña enviando la actual y la nueva.
  changeMyPassword(
    currentPassword: string,
    newPassword: string,
  ): Observable<void> {
    return this.http.put<void>(`${this.apiUrl}/me/password`, {
      currentPassword,
      newPassword,
    });
  }
}
