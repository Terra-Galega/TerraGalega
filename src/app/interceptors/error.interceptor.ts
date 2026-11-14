import { HttpErrorResponse, HttpInterceptorFn } from '@angular/common/http';
import { inject } from '@angular/core';
import { Router } from '@angular/router';
import { catchError, throwError } from 'rxjs';
import { AuthService } from '../services/auth.service';
import { getErrorMessage } from '../utils/http-error';

// Peticiones cuyo error NO maneja este interceptor:
// /auth/login: un 401 significa "contraseña incorrecta" (lo muestra el formulario).
// /auth/me: la revisión periódica maneja su propio 401 en AuthService.
const IGNORED_URLS = ['/auth/login', '/auth/me'];

// Atiende en un solo lugar los errores que ninguna pantalla debería resolver
// sola: 401 cierra la sesión y 403 lleva a la página de error.
export const errorInterceptor: HttpInterceptorFn = (req, next) => {
    const auth = inject(AuthService);
    const router = inject(Router);

    return next(req).pipe(
        catchError((error: HttpErrorResponse) => {
            const ignored = IGNORED_URLS.some((url) => req.url.includes(url));

            if (!ignored && error.status === 401) {
                auth.logout();
                router.navigate(['/login'], { replaceUrl: true });
            }

            if (!ignored && error.status === 403) {
                router.navigate(['/error'], {
                    replaceUrl: true,
                    queryParams: { status: 403, message: getErrorMessage(error) },
                });
            }

            // Se vuelve a lanzar para que cada pantalla pueda mostrar su mensaje
            return throwError(() => error);
        }),
    );
};