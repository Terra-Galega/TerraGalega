import { Injectable } from '@angular/core';
import { Admin } from '../models/admin';

@Injectable({
  providedIn: 'root'
})
export class AdminService {

  private admins: Admin[] = [
    {
      id: 1,
      name: 'Administrador',
      lastName: '',
      email: 'admin@terra.com',
      password: 'admin123'
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
}