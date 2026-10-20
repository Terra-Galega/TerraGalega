import { inject } from '@angular/core';
import { CanActivateFn, Router } from '@angular/router';
import { AuthService } from '../services/auth.service';

// Páginas generales (home, menú, login, etc.):
// un administrador no puede entrar, lo devuelve a su panel
export const clientAreaGuard: CanActivateFn = () => {
  const auth = inject(AuthService);
  const router = inject(Router);

  const session = auth.session;

  if (session && session.type === 'admin') {
    return router.createUrlTree(['/admin', session.id]);
  }
  if (session && session.type === 'operator') {
    return router.createUrlTree(['/operator', session.id]);
  }

  return true;
};

// Panel admin: solo un administrador con sesión y solo su propio panel
export const adminGuard: CanActivateFn = (route) => {
  const auth = inject(AuthService);
  const router = inject(Router);

  const session = auth.session;

  if (!session || session.type !== 'admin') {
    return router.createUrlTree(['/login']);
  }

  if (Number(route.paramMap.get('id')) !== session.id) {
    return router.createUrlTree(['/admin', session.id]);
  }

  return true;
};

export const operatorGuard: CanActivateFn = (route) => {
  const auth = inject(AuthService);
  const router = inject(Router);
  const session = auth.session;

  if (!session || session.type !== 'operator') {
    return router.createUrlTree(['/login']);
  }
  if (Number(route.paramMap.get('id')) !== session.id) {
    return router.createUrlTree(['/operator', session.id]);
  }
  return true;
};
