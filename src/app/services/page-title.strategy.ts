import { LiveAnnouncer } from '@angular/cdk/a11y';
import { effect, Injectable, inject, signal } from '@angular/core';
import { Title } from '@angular/platform-browser';
import { RouterStateSnapshot, TitleStrategy } from '@angular/router';
import { LanguageService } from './language.service';

const APP_NAME = 'Terra Galega';

// Título de la pestaña y aviso para lectores de pantalla al cambiar de página.
//
// En una SPA el navegador no recarga, así que el lector de pantalla no se entera de que la
// página cambió. Esta estrategia, en cada navegación:
//   1. Pone el título de la ruta (el "title" de app.routes.ts): "Menú · Terra Galega".
//   2. Lo anuncia en voz ("Menú") con LiveAnnouncer.
//   3. Lleva el foco al inicio del contenido nuevo (el <h1> o, si no hay, el <main>), para que
//      quien navega con teclado o lector siga leyendo desde ahí y no desde el final de la navbar.
@Injectable({ providedIn: 'root' })
export class PageTitleStrategy extends TitleStrategy {
  private title = inject(Title);
  private announcer = inject(LiveAnnouncer);
  private language = inject(LanguageService);
  private currentPage = signal<string | undefined>(undefined);
  private first = true;

  constructor() {
    super();
    effect(() => {
      this.language.language();
      const page = this.currentPage();
      this.title.setTitle(page ? `${this.language.t(page)} · ${APP_NAME}` : APP_NAME);
    });
  }

  override updateTitle(snapshot: RouterStateSnapshot): void {
    const page = this.buildTitle(snapshot);
    this.currentPage.set(page);
    this.title.setTitle(page ? `${this.language.t(page)} · ${APP_NAME}` : APP_NAME);

    // En la primera carga el lector ya lee la página entera: no hace falta moverse
    if (this.first) {
      this.first = false;
      return;
    }

    if (page) {
      void this.announcer.announce(this.language.t(page), 'polite');
    }
    // Espera a que la página nueva esté pintada (y al fundido entre páginas)
    setTimeout(() => focusMainContent(), 350);
  }
}

// Enfoca el título principal de la página (o el <main>) sin desplazar la vista
export function focusMainContent(): void {
  if (typeof document === 'undefined') return;
  const target =
    document.querySelector<HTMLElement>('main h1, h1') ??
    document.querySelector<HTMLElement>('main, [role="main"]');
  if (!target) return;
  // tabindex -1: se puede enfocar por código pero no entra en el orden del Tab
  if (!target.hasAttribute('tabindex')) {
    target.setAttribute('tabindex', '-1');
  }
  target.classList.add('focus-target');
  target.focus({ preventScroll: true });
}
