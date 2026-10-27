import { Injectable } from '@angular/core';
import { Admin } from '../models/admin';
import { Role } from '../models/role';
import { HttpClient } from '@angular/common/http';
import { ClientDTO } from '../models/client';

// Servicio encargado de gestionar los administradores y consultar clientes.
@Injectable({
  providedIn: 'root',
})
export class AdminService {
  // Cliente HTTP utilizado para realizar solicitudes al backend.
  constructor(private http: HttpClient) {}

  // Lista local de administradores utilizada para las operaciones actuales.
  private admins: Admin[] = [
    {
      id: 1,
      name: 'Admin',
      lastName: 'Terra',
      email: 'admin@terra.com',
      password: 'admin123',
      role: Role.ADMIN
    }
  ];

  // Busca un administrador que coincida con el correo y la contraseña ingresados.
  // Devuelve el administrador encontrado o undefined si no existe coincidencia.
  login(email: string, password: string): Admin | undefined {
    return this.admins.find(
      admin =>
        admin.email === email &&
        admin.password === password
    );
  }

  // Devuelve la lista local de administradores.
  getAdmins(): Admin[] {
    return this.admins;
  }

  // Consulta al backend la lista de clientes en formato DTO para el panel de administración.
  getClientDTOAdmins() {
    return this.http.get<ClientDTO[]>('http://localhost:8080/products/clients');
  }

}
