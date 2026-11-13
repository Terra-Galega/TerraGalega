import { ApplicationConfig, provideZoneChangeDetection } from '@angular/core';
import { provideRouter, withInMemoryScrolling } from '@angular/router';
import { provideHttpClient, withInterceptors } from '@angular/common/http';
import { errorInterceptor } from './interceptors/error.interceptor';
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
    ),
    // Activa HttpClient para consumir la API y le añade el interceptor de errores a todas las peticiones.
    provideHttpClient(withInterceptors([errorInterceptor])),
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
