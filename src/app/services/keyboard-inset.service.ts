import { Injectable } from '@angular/core';

// Teclado del celular y chats flotantes.
//
// En Android (Chrome), el meta viewport con interactive-widget=resizes-content ya hace que el
// teclado achique la pantalla visible. En iPhone (Safari) no: el teclado tapa la parte de abajo
// y los paneles fijos (los chats) quedan debajo de él.
//
// Este servicio mide la zona visible real (visualViewport) y deja dos variables en <html>:
//   --vv-height: alto visible (descontando el teclado)
//   --kb-inset:  cuánto tapa el teclado por abajo (0 si no hay teclado o si ya se achicó)
// Los chats las usan para subir y encogerse por encima del teclado, como en una app.
@Injectable({ providedIn: 'root' })
export class KeyboardInsetService {
  private started = false;

  init(): void {
    if (this.started || typeof window === 'undefined' || !window.visualViewport) return;
    this.started = true;

    const vv = window.visualViewport;
    const root = document.documentElement;
    const update = () => {
      const inset = Math.max(0, window.innerHeight - vv.height - vv.offsetTop);
      root.style.setProperty('--vv-height', `${Math.round(vv.height)}px`);
      root.style.setProperty('--kb-inset', `${Math.round(inset)}px`);
    };

    vv.addEventListener('resize', update);
    vv.addEventListener('scroll', update);
    update();
  }
}
