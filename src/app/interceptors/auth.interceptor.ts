import { HttpInterceptorFn } from '@angular/common/http';
import { inject } from '@angular/core';
import { API_URL, AuthService } from '../services/auth.service';

// Agrega el JWT a las peticiones que van al back, en el header
// Authorization: Bearer <token>. Así ningún servicio tiene que enviarlo a mano.
// Las peticiones de Angular no se pueden modificar, por eso se clona.
// A otros sitios no se les envía el token.
export const authInterceptor: HttpInterceptorFn = (req, next) => {
  const token = inject(AuthService).getToken();

  if (token && req.url.startsWith(API_URL)) {
    const authReq = req.clone({
      setHeaders: { Authorization: `Bearer ${token}` },
    });
    return next(authReq);
  }

  return next(req);
};
