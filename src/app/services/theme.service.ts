import { Injectable, signal } from '@angular/core';

// Temas de la app: el claro de siempre y el oscuro.
export type Theme = 'light' | 'dark';

// Clave de localStorage donde se guarda la elección (index.html la lee antes de pintar la
// página para que no haya un destello claro al recargar con el tema oscuro).
const THEME_KEY = 'terragalega_theme';

// Maneja el tema de la app. Pone el atributo data-theme="light|dark" en <html>, y desde ahí
// styles.scss cambia todos los colores. Se usa desde el switch de "Mi cuenta".
// La elección se guarda en este navegador; si nunca se eligió, se usa el tema claro.
// Lo que tarda la bolita del interruptor en deslizarse (theme-switch.component.scss)
const SWITCH_ANIMATION_MS = 280;

@Injectable({ providedIn: 'root' })
export class ThemeService {
  // Tema activo (signal: los componentes que lo leen se actualizan solos)
  readonly theme = signal<Theme>(this.read());

  constructor() {
    this.apply(this.theme());
  }

  get isDark(): boolean {
    return this.theme() === 'dark';
  }

  // Cambia al tema indicado y lo recuerda
  set(theme: Theme): void {
    const changed = theme !== this.theme();
    this.theme.set(theme);
    this.applyWithTransition(theme, changed);
    try {
      localStorage.setItem(THEME_KEY, theme);
    } catch {
      // Sin almacenamiento (modo privado): el tema solo dura mientras la pestaña esté abierta
    }
  }

  // Alterna entre claro y oscuro (el switch)
  toggle(): void {
    this.set(this.isDark ? 'light' : 'dark');
  }

  private read(): Theme {
    try {
      const saved = localStorage.getItem(THEME_KEY);
      if (saved === 'dark' || saved === 'light') {
        return saved;
      }
    } catch {
      // Sin acceso al almacenamiento
    }
    return 'light';
  }

  // Al cambiar de tema, la página se funde suavemente al nuevo (View Transitions). Sin soporte
  // del navegador o con "reducir movimiento", el cambio es directo como antes.
  private applyWithTransition(theme: Theme, changed: boolean): void {
    const doc = typeof document === 'undefined' ? null : document;
    const start = doc && 'startViewTransition' in doc
      ? (doc as Document & { startViewTransition(cb: () => void): { finished: Promise<void> } }).startViewTransition.bind(doc)
      : null;
    const reduce = typeof window !== 'undefined' && window.matchMedia('(prefers-reduced-motion: reduce)').matches;

    if (!changed || !start || reduce) {
      this.apply(theme);
      return;
    }

    // Primero se ve la bolita del interruptor deslizarse; luego la página se funde al tema nuevo
    const root = doc!.documentElement;
    setTimeout(() => {
      root.classList.add('theme-transition');
      start(() => this.apply(theme)).finished.finally(() => root.classList.remove('theme-transition'));
    }, SWITCH_ANIMATION_MS);
  }

  // Marca <html>: data-theme activa los estilos y color-scheme hace que los controles del
  // navegador (calendario, scroll, casillas) también se vean oscuros.
  private apply(theme: Theme): void {
    if (typeof document === 'undefined') {
      return;
    }
    const root = document.documentElement;
    root.setAttribute('data-theme', theme);
    root.style.colorScheme = theme;
  }
}
