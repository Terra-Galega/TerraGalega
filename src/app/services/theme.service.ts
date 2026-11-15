import { Injectable, signal } from '@angular/core';

// Temas de la app: el claro de siempre y el oscuro.
export type Theme = 'light' | 'dark';

// Clave de localStorage donde se guarda la elección (index.html la lee antes de pintar la
// página para que no haya un destello claro al recargar con el tema oscuro).
const THEME_KEY = 'terragalega_theme';

// Maneja el tema de la app. Pone el atributo data-theme="light|dark" en <html>, y desde ahí
// styles.scss cambia todos los colores. Se usa desde el switch de "Mi cuenta".
// La elección se guarda en este navegador; si nunca se eligió, se usa el tema claro.
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
    this.theme.set(theme);
    this.apply(theme);
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
