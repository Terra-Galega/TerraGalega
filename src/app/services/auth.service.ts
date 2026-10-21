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

// Llave con la que se guarda la sesión en el navegador
const SESSION_KEY = 'terragalega_session';

@Injectable({ providedIn: 'root' })
export class AuthService {
  private http = inject(HttpClient);

  private apiUrl = 'http://localhost:8080/auth';

  // La sesión se guarda en localStorage: si se recarga la página,
  // se recupera. Solo guarda id, nombre y tipo, nunca la contraseña.
  private sessionSubject = new BehaviorSubject<Session | null>(
    this.loadSession(),
  );

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
      this.updateSession(null);
      return;
    }

    this.updateSession({
      id: user.id,
      name: user.name,
      lastName: user.lastName,
      type,
    });
  }

  logout() {
    this.updateSession(null);
  }

  // Punto único de cambio: avisa a los suscriptores y guarda en el navegador
  private updateSession(session: Session | null) {
    this.sessionSubject.next(session);

    try {
      if (session) {
        localStorage.setItem(SESSION_KEY, JSON.stringify(session));
      } else {
        localStorage.removeItem(SESSION_KEY);
      }
    } catch {
      // Si el navegador bloquea el almacenamiento, la sesión sigue en memoria
    }
  }

  // Recupera la sesión guardada; si no hay o está dañada, no hay sesión
  private loadSession(): Session | null {
    try {
      const raw = localStorage.getItem(SESSION_KEY);
      if (!raw) return null;

      const parsed = JSON.parse(raw);
      const validType = ['admin', 'client', 'operator', 'delivery'].includes(
        parsed?.type,
      );

      return validType && typeof parsed.id === 'number' ? parsed : null;
    } catch {
      return null;
    }
  }
}
