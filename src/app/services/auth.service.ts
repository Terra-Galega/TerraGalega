import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { BehaviorSubject, Observable, tap } from 'rxjs';

export interface Session {
  id: number;
  name: string;
  lastName: string;
  type: 'admin' | 'client' | 'operator' | 'delivery';
}

// Rol que manda el back -> tipo de sesión que usa el front
const TYPES: Record<string, Session['type']> = {
  ADMIN: 'admin',
  CLIENT: 'client',
  OPERATOR: 'operator',
  DELIVERY_PERSON: 'delivery',
};

@Injectable({ providedIn: 'root' })
export class AuthService {
  private http = inject(HttpClient);

  private apiUrl = 'http://localhost:8080/auth';

  // La sesión vive solo en memoria: si se recarga la página, se pierde
  private sessionSubject = new BehaviorSubject<Session | null>(null);

  session$ = this.sessionSubject.asObservable();

  get session(): Session | null {
    return this.sessionSubject.value;
  }

  get isLoggedIn(): boolean {
    return this.session !== null;
  }

  // Le pregunta al back si el correo y la contraseña existen.
  // Si no existen, el back responde con error y el componente lo muestra.
  login(email: string, password: string): Observable<any> {
    return this.http.post<any>(`${this.apiUrl}/login`, { email, password });
  }

  // Se llama desde el login, el registro y editar perfil
  setSession(user: any) {
    const type = TYPES[user.role];

    if (!type) {
      this.sessionSubject.next(null);
      return;
    }

    this.sessionSubject.next({
      id: user.id,
      name: user.name,
      lastName: user.lastName,
      type,
    });
  }

  logout() {
    this.sessionSubject.next(null);
  }
}
