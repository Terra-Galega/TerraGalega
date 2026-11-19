import { bootstrapApplication } from '@angular/platform-browser';
import { appConfig } from './app/app.config';
import { AppComponent } from './app/app.component';

// LOGIN CON MICROSOFT DESACTIVADO (no se puede usar).
// La universidad no nos da permisos en Azure (Microsoft Entra ID) para registrar la
// aplicación y obtener el Client ID, así que no lo podemos usar. También se intentó
// con una cuenta personal de Microsoft, pero tampoco lo permitió.
// Se deja comentado por si en el futuro se consiguen los permisos.
// (Este bloque evitaba arrancar Angular dentro de la ventana emergente de Microsoft.)
// function isMicrosoftPopupReturn(): boolean {
  // if (window.location.pathname !== '/login' || !window.opener) return false;
//
  // const response = new URLSearchParams(window.location.hash.slice(1));
  // return response.has('state') && (response.has('code') || response.has('error'));
// }
//
// // MSAL lee la respuesta desde la ventana principal antes de cerrar el popup.
// // Arrancar el router aquí podría cambiar la URL antes de que MSAL la lea.
// if (!isMicrosoftPopupReturn()) {
  // bootstrapApplication(AppComponent, appConfig).catch(err => console.error(err));
// }

// Punto de entrada de la aplicación: inicia Angular con el componente raíz y la configuración global.
bootstrapApplication(AppComponent, appConfig)
  // Si el arranque falla, el error se muestra en la consola del navegador.
  .catch((err) => console.error(err));
