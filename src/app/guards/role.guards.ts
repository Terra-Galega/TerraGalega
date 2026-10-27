import { inject } from '@angular/core';
import { CanActivateFn, Router } from '@angular/router';
import { AuthService } from '../services/auth.service';

// Páginas generales: impide que administradores y operadores accedan a áreas que no les corresponden.
export const clientAreaGuard: CanActivateFn = () => {
  // Obtiene el servicio de autenticación y el enrutador.
  const auth = inject(AuthService);
  const router = inject(Router);

  // Consulta la sesión activa para determinar el tipo de usuario.
  const session = auth.session;

  // Redirige al administrador a su propio panel.
  if (session && session.type === 'admin') {
    return router.createUrlTree(['/admin', session.id]);
  }

  // Redirige al operador a su propio panel.
  if (session && session.type === 'operator') {
    return router.createUrlTree(['/operator', session.id]);
  }

  // Permite el acceso si no se requiere una redirección.
  return true;
};

// Protege el panel de administración y restringe el acceso a su propietario.
export const adminGuard: CanActivateFn = (route) => {
  // Obtiene los servicios necesarios para validar la sesión y redirigir.
  const auth = inject(AuthService);
  const router = inject(Router);

  // Consulta la sesión activa.
  const session = auth.session;

  // Si no hay sesión o el usuario no es administrador, lo envía al inicio de sesión.
  if (!session || session.type !== 'admin') {
    return router.createUrlTree(['/login']);
  }

  // Si el identificador de la ruta no coincide con el del administrador autenticado,
  // lo redirige a su propio panel.
  if (Number(route.paramMap.get('id')) !== session.id) {
    return router.createUrlTree(['/admin', session.id]);
  }

  // Permite el acceso al panel correspondiente.
  return true;
};

// Protege el panel del operador y restringe el acceso a su propietario.
export const operatorGuard: CanActivateFn = (route) => {
  // Obtiene los servicios de autenticación y navegación.
  const auth = inject(AuthService);
  const router = inject(Router);

  // Consulta la sesión activa.
  const session = auth.session;

  // Si no hay sesión o el usuario no es operador, lo envía al inicio de sesión.
  if (!session || session.type !== 'operator') {
    return router.createUrlTree(['/login']);
  }

  // Si el identificador de la ruta no corresponde al operador autenticado,
  // lo redirige a su propio panel.
  if (Number(route.paramMap.get('id')) !== session.id) {
    return router.createUrlTree(['/operator', session.id]);
  }

  // Permite el acceso al panel del operador correspondiente.
  return true;
};
