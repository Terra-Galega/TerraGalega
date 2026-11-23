import { ApplicationConfig, inject, provideZoneChangeDetection } from '@angular/core';
import { PreloadAllModules, Router, TitleStrategy, provideRouter, withInMemoryScrolling, withPreloading, withViewTransitions } from '@angular/router';
import { PageTitleStrategy } from './strategies/page-title.strategy';
import { provideHttpClient, withInterceptors } from '@angular/common/http';
import { errorInterceptor } from './interceptors/error.interceptor';
import { authInterceptor } from './interceptors/auth.interceptor';
import { busyInterceptor } from './interceptors/busy.interceptor';
import { routes } from './app.routes';

// Configuración global de la aplicación, que se pasa a bootstrapApplication en main.ts.
export const appConfig: ApplicationConfig = {
  // Lista de proveedores (servicios y funcionalidades) disponibles para toda la app.
  providers: [
    // Agrupa varios eventos del DOM que ocurren a la vez en una sola detección de cambios, mejorando el rendimiento.
    provideZoneChangeDetection({ eventCoalescing: true }),
    // Activa el enrutamiento con las rutas definidas y opciones adicionales.
    provideRouter(
      routes,
      // Descarga en segundo plano las páginas diferidas (menú, checkout, admin...) apenas la
      // app queda en reposo: al navegar ya están listas y no hay pantalla en blanco.
      withPreloading(PreloadAllModules),
      // al cambiar de página siempre se empieza desde arriba
      withInMemoryScrolling({ scrollPositionRestoration: 'top' }),
      // Fundido entre páginas (el estilo está en styles.scss). Si solo cambian los parámetros
      // de la misma página (filtros, pestañas), no hay fundido.
      withViewTransitions({
        skipInitialTransition: true,
        onViewTransitionCreated: ({ transition }) => {
          const router = inject(Router);
          const target = router.getCurrentNavigation()?.finalUrl;
          const samePage =
            target &&
            router.isActive(target, {
              paths: 'exact',
              matrixParams: 'exact',
              fragment: 'ignored',
              queryParams: 'ignored',
            });
          if (samePage) transition.skipTransition();
          // Seguro: si el navegador no llega a pintar el fundido (pestaña en segundo plano),
          // se salta para que el cambio de página nunca se quede esperando.
          const guard = setTimeout(() => transition.skipTransition(), 1500);
          transition.finished.finally(() => clearTimeout(guard)).catch(() => {});
          // Al saltar el fundido, el navegador rechaza también la promesa "ready" con un
          // AbortError; es lo esperado, así que se ignora para que no ensucie la consola.
          transition.ready.catch(() => {});
        },
      }),
    ),
    // Título de cada página ("Menú - Terra Galega") y aviso al lector de pantalla al navegar
    { provide: TitleStrategy, useClass: PageTitleStrategy },
    // Activa HttpClient para consumir la API con dos interceptores, que se ejecutan en orden:
    // busyInterceptor deshabilita los botones mientras haya un POST/PUT/PATCH/DELETE en curso;
    // authInterceptor agrega el JWT a cada petición; errorInterceptor maneja los 401/403.
    provideHttpClient(withInterceptors([busyInterceptor, authInterceptor, errorInterceptor])),
  ],
};
