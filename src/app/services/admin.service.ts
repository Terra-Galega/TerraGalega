import { API_URL } from '../api-url';
import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { ClientDTO } from '../models/client';

// Servicio encargado de gestionar los administradores y consultar clientes.
@Injectable({
  providedIn: 'root',
})
export class AdminService {
  // Cliente HTTP utilizado para realizar solicitudes al backend.
  constructor(private http: HttpClient) {}

  // Consulta al backend la lista de clientes en formato DTO para el panel de administración.
  getClientDTOAdmins() {
    return this.http.get<ClientDTO[]>(`${API_URL}/products/clients`);
  }

}
