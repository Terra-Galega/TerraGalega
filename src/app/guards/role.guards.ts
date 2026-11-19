import { inject } from '@angular/core';
import { CanActivateFn, Router } from '@angular/router';
import { AuthService, SessionType } from '../services/auth.service';

// Los guards deciden si se puede entrar a una ruta o a dónde redirigir.
// Solo controlan lo que se muestra en el front; los permisos reales los valida
// el back con el token en cada petición.

// Inicio y login: si ya hay una sesión, se va directo a la página del rol.
export const redirectByRoleGuard: CanActivateFn = () => {
  const auth = inject(AuthService);
  const router = inject(Router);

  if (auth.isLoggedIn) {
    return router.createUrlTree([auth.homeRoute()]);
  }

  return true;
};

// Páginas públicas (menú, nosotros, pedidos, checkout...): cualquiera entra,
// pero el admin y el operador se mandan a su propio panel.
export const clientAreaGuard: CanActivateFn = () => {
  const auth = inject(AuthService);
  const router = inject(Router);

  if (!auth.isLoggedIn) {
    return true;
  }

  const type = auth.session!.type;
  if (type === 'admin' || type === 'operator' || type === 'kitchen') {
    return router.createUrlTree([auth.homeRoute()]);
  }

  // Cuenta de Google sin terminar: primero completa su perfil
  if (auth.needsProfile) {
    return router.createUrlTree(['/complete-profile']);
  }

  return true;
};

// "Completa tu perfil": solo clientes a los que les faltan datos
export const completeProfileGuard: CanActivateFn = () => {
  const auth = inject(AuthService);
  const router = inject(Router);

  if (!auth.isLoggedIn) {
    return router.createUrlTree(['/login']);
  }
  if (!auth.needsProfile) {
    return router.createUrlTree([auth.homeRoute()]);
  }
  return true;
};

// Crea un guard que exige un rol: sin sesión va al login
// y con otro rol va a la página de su rol.
function requireRole(type: SessionType): CanActivateFn {
  return () => {
    const auth = inject(AuthService);
    const router = inject(Router);

    if (!auth.isLoggedIn) {
      return router.createUrlTree(['/login']);
    }

    if (auth.session!.type !== type) {
      return router.createUrlTree([auth.homeRoute()]);
    }

    if (auth.needsProfile) {
      return router.createUrlTree(['/complete-profile']);
    }

    return true;
  };
}

// Panel de administración
export const adminGuard = requireRole('admin');

// Panel del operador
export const operatorGuard = requireRole('operator');

// Portal de cocina (solo la cuenta de cocina)
export const kitchenGuard = requireRole('kitchen');

// Páginas del cliente con sesión (cuenta, editar cuenta, cupones)
export const clientGuard = requireRole('client');
