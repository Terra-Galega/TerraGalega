import { Injectable } from '@angular/core';
import { User } from '../models/user';

@Injectable({
  providedIn: 'root'
})
export class UserService {

  private users: User[] = [
    {
      id: 1,
      name: 'María',
      lastName: 'García',
      email: 'cliente@terra.com',
      password: 'cliente123'
    }
  ];

  login(email: string, password: string): User | undefined {
    return this.users.find(
      user =>
        user.email === email &&
        user.password === password
    );
  }

  getUsers(): User[] {
    return this.users;
  }
}