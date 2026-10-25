import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Router } from '@angular/router';
import {
  BehaviorSubject,
  EMPTY,
  Observable,
  catchError,
  filter,
  fromEvent,
  merge,
  switchMap,
  timer,
} from 'rxjs';

export interface Session {
  id: number;
  name: string;
  lastName: string;
  address?: string;
  type: 'admin' | 'client' | 'operator';
}

// Rol que manda el back -> tipo de sesión que usa el front
const TYPES: Record<string, Session['type']> = {
  ADMIN: 'admin',
  CLIENT: 'client',
  OPERATOR: 'operator',
};

// Llave con la que se guarda la sesión en el navegador
const SESSION_KEY = 'terragalega_session';

// Cada cuánto se revisa que la cuenta con sesión iniciada siga existiendo
const SESSION_CHECK_MS = 15_000;
@Injectable({ providedIn: 'root' })
export class AuthService {
  private http = inject(HttpClient);
  private router = inject(Router);

  private watching = false;

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
      address: user.address,
    });
  }

  logout() {
    this.updateSession(null);
  }

  // Si el admin elimina la cuenta de alguien que tiene la sesión abierta, se
  // cierra su sesión y se le lleva al inicio. Se revisa al abrir la página,
  // cada SESSION_CHECK_MS y al volver a la pestaña
  startSessionWatch() {
    if (this.watching) {
      return;
    }
    this.watching = true;

    this.session$
      .pipe(
        switchMap((session) => {
          if (!session) {
            return EMPTY;
          }

          return merge(
            timer(0, SESSION_CHECK_MS),
            fromEvent(document, 'visibilitychange'),
          ).pipe(
            filter(() => document.visibilityState === 'visible'),
            switchMap(() =>
              this.http
                .get<{
                  exists: boolean;
                }>(`${this.apiUrl}/session/${session.type}/${session.id}`)
                .pipe(catchError(() => EMPTY)),
            ),
            filter((response) => response.exists === false),
          );
        }),
      )
      .subscribe(() => {
        this.logout();
        this.router.navigate(['/home'], { replaceUrl: true });
      });
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
      const validType = ['admin', 'client', 'operator'].includes(
        parsed?.type,
      );

      return validType && typeof parsed.id === 'number' ? parsed : null;
    } catch {
      return null;
    }
  }
}