import { Injectable } from '@angular/core';
import { Admin } from '../models/admin';
import { Role } from '../models/role';
import { HttpClient } from '@angular/common/http';
import { ClientDTO } from '../models/client';

@Injectable({
  providedIn: 'root',
})
export class AdminService {
  constructor(private http: HttpClient) {}
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

  login(email: string, password: string): Admin | undefined {
    return this.admins.find(
      admin =>
        admin.email === email &&
        admin.password === password
    );
  }

  getAdmins(): Admin[] {
    return this.admins;
  }
  getClientDTOAdmins() {
    return this.http.get<ClientDTO[]>('http://localhost:8080/products/clients');
  }

}
