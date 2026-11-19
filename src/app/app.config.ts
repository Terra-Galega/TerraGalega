import { ApplicationConfig, inject, provideZoneChangeDetection } from '@angular/core';
import { Router, TitleStrategy, provideRouter, withInMemoryScrolling, withViewTransitions } from '@angular/router';
import { PageTitleStrategy } from './services/page-title.strategy';
import { provideHttpClient, withInterceptors } from '@angular/common/http';
import { errorInterceptor } from './interceptors/error.interceptor';
import { authInterceptor } from './interceptors/auth.interceptor';
import { busyInterceptor } from './interceptors/busy.interceptor';
import { routes } from './app.routes';
import {
  ArcElement,
  BarController,
  BarElement,
  CategoryScale,
  DoughnutController,
  Filler,
  LineController,
  LineElement,
  LinearScale,
  PointElement,
  Tooltip,
} from 'chart.js';
import { provideCharts } from 'ng2-charts';

// Configuración global de la aplicación, que se pasa a bootstrapApplication en main.ts.
export const appConfig: ApplicationConfig = {
  // Lista de proveedores (servicios y funcionalidades) disponibles para toda la app.
  providers: [
    // Agrupa varios eventos del DOM que ocurren a la vez en una sola detección de cambios, mejorando el rendimiento.
    provideZoneChangeDetection({ eventCoalescing: true }),
    // Activa el enrutamiento con las rutas definidas y opciones adicionales.
    provideRouter(
      routes,
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
        },
      }),
    ),
    // Título de cada página ("Menú - Terra Galega") y aviso al lector de pantalla al navegar
    { provide: TitleStrategy, useClass: PageTitleStrategy },
    // Activa HttpClient para consumir la API con dos interceptores, que se ejecutan en orden:
    // busyInterceptor deshabilita los botones mientras haya un POST/PUT/PATCH/DELETE en curso;
    // authInterceptor agrega el JWT a cada petición; errorInterceptor maneja los 401/403.
    provideHttpClient(withInterceptors([busyInterceptor, authInterceptor, errorInterceptor])),
    // Chart.js: se registran solo las piezas que usa el dashboard (barras, líneas,
    // dona, ejes y tooltip) para no cargar toda la librería en el bundle.
    provideCharts({
      registerables: [
        BarController,
        BarElement,
        LineController,
        LineElement,
        PointElement,
        DoughnutController,
        ArcElement,
        CategoryScale,
        LinearScale,
        Filler,
        Tooltip,
      ],
    }),
  ],
};
