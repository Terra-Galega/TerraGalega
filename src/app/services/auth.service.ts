import { Injectable, inject } from '@angular/core';
import { HttpClient, HttpErrorResponse } from '@angular/common/http';
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
  tap,
  timer,
} from 'rxjs';
import { CreateClient } from '../models/client';

// Maneja la sesión y el JWT en el front: lo guarda al iniciar sesión, lo entrega
// al interceptor, revisa si venció y lo borra al cerrar sesión.
// El front no puede verificar la firma del token (eso lo hace el back);
// solo lee sus datos para saber si venció y qué rol tiene el usuario.

// Tipos de sesión de la web (el domiciliario usa la app móvil).
export type SessionType = 'admin' | 'client' | 'operator' | 'kitchen';

// Datos de la sesión que usa el front para mostrar el nombre y decidir rutas.
export interface Session {
  id: number;
  name: string;
  lastName: string;
  address?: string;
  type: SessionType;
  // false = cuenta creada con Google a la que le faltan teléfono, apellido o
  // dirección. Mientras tanto se le muestra "Completa tu perfil".
  profileComplete?: boolean;
}

// Respuesta del back al iniciar sesión, registrarse o editar el perfil.
export interface LoginResponse {
  token: string;
  id: number;
  name: string;
  lastName: string;
  role: string;
  profileComplete?: boolean;
}

// Respuesta del back al registrarse, confirmar el correo o pedir un reenvío:
// solo un texto para mostrar al usuario.
export interface MessageResponse {
  message: string;
}

// Datos que vienen dentro del JWT.
interface JwtPayload {
  sub: string; // correo del usuario
  roles?: string[];
  iat: number; // fecha de creación, en segundos
  exp: number; // fecha de vencimiento, en segundos
}

// URL del back (se calcula en api-url.ts). Se reexporta aquí porque otros archivos la importan de este servicio.
import { API_URL } from '../api-url';
export { API_URL };

const SESSION_KEY = 'terragalega_session';
const TOKEN_KEY = 'terragalega_token';

// Cada cuánto se revisa que el token siga vigente y que la cuenta exista.
const SESSION_CHECK_MS = 15_000;

// Si al token le queda menos de esto, se pide uno nuevo sin cerrar la sesión.
const RENEW_BEFORE_MS = 60 * 60 * 1000; // 1 hora

// Página principal de cada rol.
const HOME_BY_TYPE: Record<SessionType, string> = {
  admin: '/admin',
  operator: '/operator',
  client: '/account',
  // Cuenta de cocina: su propio portal con los pedidos de mesa
  kitchen: '/kitchen',
};

// Convierte el rol del back en el tipo de sesión del front.
function sessionType(role: string | undefined): SessionType | null {
  switch (role) {
    case 'ADMIN':
      return 'admin';
    case 'CLIENT':
      return 'client';
    case 'OPERATOR':
      return 'operator';
    case 'KITCHEN':
      return 'kitchen';
    default:
      return null;
  }
}

@Injectable({ providedIn: 'root' })
export class AuthService {
  private http = inject(HttpClient);
  private router = inject(Router);

  // Evita iniciar más de una vez la revisión periódica.
  private watching = false;

  private apiUrl = `${API_URL}/auth`;

  // Al recargar la página, la sesión solo se recupera si hay un token vigente.
  private sessionSubject = new BehaviorSubject<Session | null>(
    this.loadSession(),
  );

  session$ = this.sessionSubject.asObservable();

  get session(): Session | null {
    return this.sessionSubject.value;
  }

  // Si el token venció, la sesión se cierra en este momento.
  get isLoggedIn(): boolean {
    if (this.session && !this.getToken()) {
      this.logout();
    }
    return this.session !== null;
  }

  // =========================
  // Peticiones de autenticación
  // =========================

  login(email: string, password: string): Observable<LoginResponse> {
    return this.http.post<LoginResponse>(`${this.apiUrl}/login`, {
      email,
      password,
    });
  }

  // "Continuar con Google": el back verifica el token de Google y crea la
  // cuenta o inicia sesión. Si profileComplete es false hay que completar el perfil.
  googleLogin(idToken: string): Observable<LoginResponse> {
    return this.http.post<LoginResponse>(`${this.apiUrl}/google`, { idToken });
  }

  microsoftLogin(idToken: string, confirmationCode?: string): Observable<LoginResponse> {
    return this.http.post<LoginResponse>(`${this.apiUrl}/microsoft`, { idToken, confirmationCode });
  }

  // ¿El cliente con sesión debe completar su perfil antes de seguir?
  get needsProfile(): boolean {
    return this.session?.type === 'client' && this.session.profileComplete === false;
  }

  // Registra un cliente. No inicia sesión: el back envía un correo y la cuenta
  // solo entra después de confirmarlo.
  signup(client: CreateClient): Observable<MessageResponse> {
    return this.http.post<MessageResponse>(`${this.apiUrl}/signup`, client);
  }

  // Confirma el correo con el token que llegó en el enlace.
  verifyEmail(token: string): Observable<MessageResponse> {
    return this.http.post<MessageResponse>(`${this.apiUrl}/verify-email`, {
      token,
    });
  }

  // Pide un nuevo correo de confirmación (el back responde igual exista o no la cuenta).
  resendVerification(email: string): Observable<MessageResponse> {
    return this.http.post<MessageResponse>(
      `${this.apiUrl}/resend-verification`,
      { email },
    );
  }

  // =========================
  // Sesión y token
  // =========================

  // Crea o actualiza la sesión. Si la respuesta trae un token lo guarda;
  // si no lo trae, se conserva el que ya había.
  setSession(user: any) {
    if (user?.token) {
      this.saveToken(user.token);
    }

    const type = sessionType(user?.role);

    if (!type || !this.getToken()) {
      this.logout();
      return;
    }

    this.updateSession({
      id: user.id,
      name: user.name,
      lastName: user.lastName,
      type,
      address: user.address,
      // Las respuestas que no traen el dato (p. ej. cuentas normales) cuentan como completas
      profileComplete: user.profileComplete !== false,
    });
  }

  // Devuelve el token solo si existe y no ha vencido.
  getToken(): string | null {
    const token = this.readToken();
    if (!token) {
      return null;
    }
    const payload = this.decode(token);
    // exp está en segundos y Date.now() en milisegundos
    if (!payload || payload.exp * 1000 <= Date.now()) {
      return null;
    }
    return token;
  }

  // Página a la que va cada rol; sin sesión, el inicio.
  homeRoute(type: SessionType | undefined = this.session?.type): string {
    return type ? HOME_BY_TYPE[type] : '/home';
  }

  // Con JWT el servidor no guarda la sesión: basta con borrar el token del navegador.
  logout() {
    this.removeToken();
    this.updateSession(null);
  }

  // Cada cierto tiempo, y al volver a la pestaña, revisa que el token siga vigente
  // y que la cuenta exista. Si no, cierra la sesión.
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
            switchMap(() => {
              if (!this.getToken()) {
                this.endSession();
                return EMPTY;
              }
              // Al token le queda poco: se pide uno nuevo y se sobrescribe el anterior.
              // El interceptor lo usa desde la siguiente petición, sin cerrar la sesión.
              if (this.expiresInMs() < RENEW_BEFORE_MS) {
                return this.http
                  .post<LoginResponse>(`${this.apiUrl}/refresh`, {})
                  .pipe(
                    // Solo se cambia el token: los datos de la sesión siguen iguales
                    // y así la revisión no se reinicia en cada renovación
                    tap((response) => this.saveToken(response.token)),
                    catchError((error: HttpErrorResponse) => {
                      if (error.status === 401) {
                        this.endSession();
                      }
                      return EMPTY;
                    }),
                  );
              }
              // El back responde 401 si el token no es válido o la cuenta ya no existe
              return this.http.get(`${this.apiUrl}/me`).pipe(
                catchError((error: HttpErrorResponse) => {
                  if (error.status === 401) {
                    this.endSession();
                  }
                  return EMPTY;
                }),
              );
            }),
          );
        }),
      )
      .subscribe();
  }

  // =========================
  // Privados
  // =========================

  private endSession() {
    this.logout();
    this.router.navigate(['/login'], { replaceUrl: true });
  }

  private updateSession(session: Session | null) {
    this.sessionSubject.next(session);
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

  // Recupera la sesión guardada solo si hay un token vigente y el rol coincide
  // con el del token (así no sirve editar el localStorage a mano).
  private loadSession(): Session | null {
    try {
      const token = this.getToken();
      if (!token) {
        localStorage.removeItem(SESSION_KEY);
        localStorage.removeItem(TOKEN_KEY);
        return null;
      }

      const raw = localStorage.getItem(SESSION_KEY);
      if (!raw) return null;

      const parsed = JSON.parse(raw);
      const tokenType = sessionType(this.decode(token)?.roles?.[0]);

      const valid =
        tokenType !== null &&
        parsed?.type === tokenType &&
        typeof parsed.id === 'number';

      return valid ? parsed : null;
    } catch {
      return null;
    }
  }

  // Milisegundos que le quedan al token (0 si no hay token o no se puede leer)
  private expiresInMs(): number {
    const token = this.readToken();
    const payload = token ? this.decode(token) : null;
    return payload ? payload.exp * 1000 - Date.now() : 0;
  }

  // Lee los datos del JWT (la parte del medio, en Base64URL). No verifica la firma.
  private decode(token: string): JwtPayload | null {
    try {
      const base64 = token.split('.')[1].replace(/-/g, '+').replace(/_/g, '/');
      const json = decodeURIComponent(
        atob(base64)
          .split('')
          .map((c) => '%' + ('00' + c.charCodeAt(0).toString(16)).slice(-2))
          .join(''),
      );
      return JSON.parse(json);
    } catch {
      return null;
    }
  }

  private readToken(): string | null {
    try {
      return localStorage.getItem(TOKEN_KEY);
    } catch {
      return null;
    }
  }

  private saveToken(token: string) {
    try {
      localStorage.setItem(TOKEN_KEY, token);
    } catch { }
  }

  private removeToken() {
    try {
      localStorage.removeItem(TOKEN_KEY);
    } catch { }
  }
}
