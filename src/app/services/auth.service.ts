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

// Representa la información de la sesión activa en el frontend.
export interface Session {
  id: number;
  name: string;
  lastName: string;
  address?: string;
  type: 'admin' | 'client' | 'operator';
}

// Define los datos que devuelve el backend al iniciar sesión.
export interface LoginResponse {
  id: number;
  name: string;
  lastName: string;
  address?: string; // Solo los clientes tienen dirección.
  role: string;
}

// Convierte los roles enviados por el backend en los tipos de sesión del frontend.
const TYPES: Record<string, Session['type']> = {
  ADMIN: 'admin',
  CLIENT: 'client',
  OPERATOR: 'operator',
};

// Clave utilizada para guardar y recuperar la sesión desde el navegador.
const SESSION_KEY = 'terragalega_session';

// Intervalo entre verificaciones de existencia de la cuenta.
const SESSION_CHECK_MS = 15_000;

@Injectable({ providedIn: 'root' })
export class AuthService {
  // Servicios necesarios para realizar solicitudes HTTP y navegar entre páginas.
  private http = inject(HttpClient);
  private router = inject(Router);

  // Evita iniciar más de una vez la verificación periódica de la sesión.
  private watching = false;

  // URL base de los endpoints de autenticación del backend.
  private apiUrl = 'http://localhost:8080/auth';

  // Mantiene la sesión actual y permite recuperarla al recargar la página.
  // Solo almacena información básica del usuario, nunca su contraseña.
  private sessionSubject = new BehaviorSubject<Session | null>(
    this.loadSession(),
  );

  // Expone los cambios de sesión como un observable para otros componentes.
  session$ = this.sessionSubject.asObservable();

  // Devuelve la sesión actual.
  get session(): Session | null {
    return this.sessionSubject.value;
  }

  // Indica si existe una sesión activa.
  get isLoggedIn(): boolean {
    return this.session !== null;
  }

  // Envía el correo y la contraseña al backend para iniciar sesión.
  // La respuesta o el error se gestionan en el componente que realiza la llamada.
  login(email: string, password: string): Observable<LoginResponse> {
    return this.http.post<LoginResponse>(`${this.apiUrl}/login`, {
      email,
      password,
    });
  }

  // Crea o actualiza la sesión a partir de los datos del usuario.
  // Se utiliza después del inicio de sesión, el registro o la edición del perfil.
  setSession(user: any) {
    const type = TYPES[user.role];

    // Si el rol no es reconocido, se elimina la sesión.
    if (!type) {
      this.updateSession(null);
      return;
    }

    // Guarda los datos necesarios para identificar al usuario en el frontend.
    this.updateSession({
      id: user.id,
      name: user.name,
      lastName: user.lastName,
      type,
      address: user.address,
    });
  }

  // Finaliza la sesión y elimina sus datos guardados.
  logout() {
    this.updateSession(null);
  }

  // Comprueba periódicamente que la cuenta asociada a la sesión siga existiendo.
  // También realiza una verificación al iniciar y al volver a la pestaña.
  startSessionWatch() {
    // Evita crear suscripciones de vigilancia duplicadas.
    if (this.watching) {
      return;
    }
    this.watching = true;

    // Reacciona a los cambios de sesión para iniciar o detener la vigilancia.
    this.session$
      .pipe(
        switchMap((session) => {
          // Si no hay una sesión activa, no se realizan verificaciones.
          if (!session) {
            return EMPTY;
          }

          // Genera verificaciones periódicas y escucha cuando cambia la visibilidad
          // de la pestaña del navegador.
          return merge(
            timer(0, SESSION_CHECK_MS),
            fromEvent(document, 'visibilitychange'),
          ).pipe(
            // Solo verifica la cuenta cuando la pestaña está visible.
            filter(() => document.visibilityState === 'visible'),

            // Consulta al backend para comprobar si la cuenta sigue existiendo.
            switchMap(() =>
              this.http
                .get<{
                  exists: boolean;
                }>(`${this.apiUrl}/session/${session.type}/${session.id}`)
                .pipe(catchError(() => EMPTY)),
            ),

            // Continúa únicamente cuando el backend indica que la cuenta no existe.
            filter((response) => response.exists === false),
          );
        }),
      )
      .subscribe(() => {
        // Cierra la sesión y redirige al inicio si la cuenta fue eliminada.
        this.logout();
        this.router.navigate(['/home'], { replaceUrl: true });
      });
  }

  // Centraliza las actualizaciones de la sesión y notifica a sus suscriptores.
  private updateSession(session: Session | null) {
    this.sessionSubject.next(session);

    // Persiste la sesión en el navegador o elimina los datos si se cerró.
    try {
      if (session) {
        localStorage.setItem(SESSION_KEY, JSON.stringify(session));
      } else {
        localStorage.removeItem(SESSION_KEY);
      }
    } catch {
      // Si el navegador bloquea el almacenamiento, la sesión sigue en memoria.
    }
  }

  // Recupera la sesión almacenada y valida sus datos básicos.
  private loadSession(): Session | null {
    try {
      const raw = localStorage.getItem(SESSION_KEY);

      // Si no existe información guardada, no hay una sesión que recuperar.
      if (!raw) return null;

      const parsed = JSON.parse(raw);

      // Comprueba que el tipo de usuario sea válido y que el identificador sea numérico.
      const validType = ['admin', 'client', 'operator'].includes(parsed?.type);

      return validType && typeof parsed.id === 'number' ? parsed : null;
    } catch {
      // Si los datos no se pueden leer o interpretar, devuelve una sesión vacía.
      return null;
    }
  }
}
