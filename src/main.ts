import { bootstrapApplication } from '@angular/platform-browser';
import { appConfig } from './app/app.config';
import { AppComponent } from './app/app.component';

function isMicrosoftPopupReturn(): boolean {
  if (window.location.pathname !== '/login' || !window.opener) return false;

  const response = new URLSearchParams(window.location.hash.slice(1));
  return response.has('state') && (response.has('code') || response.has('error'));
}

// MSAL lee la respuesta desde la ventana principal antes de cerrar el popup.
// Arrancar el router aquí podría cambiar la URL antes de que MSAL la lea.
if (!isMicrosoftPopupReturn()) {
  bootstrapApplication(AppComponent, appConfig).catch(err => console.error(err));
}
