import { Injectable } from '@angular/core';
import { BehaviorSubject } from 'rxjs';

export interface Session {
  id: number;
  name: string;
  lastName: string;
  type: 'admin' | 'client';
}

@Injectable({ providedIn: 'root' })
export class AuthService {
  private sessionSubject = new BehaviorSubject<Session | null>(this.readSession());

  session$ = this.sessionSubject.asObservable();

  get session(): Session | null {
    return this.sessionSubject.value;
  }

  get isLoggedIn(): boolean {
    return this.session !== null;
  }

  // Se llama desde el login y desde editar perfil
  setSession(user: any, type: 'admin' | 'client') {
    localStorage.setItem('currentUser', JSON.stringify(user));
    localStorage.setItem('userType', type);
    this.sessionSubject.next(this.readSession());
  }

  logout() {
    localStorage.removeItem('currentUser');
    localStorage.removeItem('currentAdmin');
    localStorage.removeItem('userType');
    this.sessionSubject.next(null);
  }

  private readSession(): Session | null {
    try {
      const raw = localStorage.getItem('currentUser');
      const type = localStorage.getItem('userType');

      if (!raw || (type !== 'admin' && type !== 'client')) {
        return null;
      }

      const user = JSON.parse(raw);

      return {
        id: user.id,
        name: user.name,
        lastName: user.lastName,
        type,
      };
    } catch {
      return null;
    }
  }
}