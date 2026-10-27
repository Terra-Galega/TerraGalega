import { bootstrapApplication } from '@angular/platform-browser';
import { appConfig } from './app/app.config';
import { AppComponent } from './app/app.component';


// Punto de entrada de la aplicación: inicia Angular con el componente raíz y la configuración global.
bootstrapApplication(AppComponent, appConfig)
  // Si el arranque falla, el error se muestra en la consola del navegador.
  .catch((err) => console.error(err));