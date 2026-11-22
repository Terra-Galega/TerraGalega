import { Injectable, computed, signal } from '@angular/core';

// Opciones de daltonismo del panel del ojo:
// - 'standard': los colores de siempre.
// - 'red-green': protanopía y deuteranopía (una sola paleta sirve para las dos).
// - 'blue-yellow': tritanopía.
export type Palette = 'standard' | 'red-green' | 'blue-yellow';

// Claves de localStorage (index.html las lee antes de pintar la página para evitar destellos)
const PALETTE_KEY = 'terragalega_palette';
const CONTRAST_KEY = 'terragalega_contrast';

// Paleta activa leída del <html>, igual que isDarkTheme(): así las funciones de gráficas
// no tienen que inyectar el servicio.
export function activePalette(): Palette {
    if (typeof document === 'undefined') {
        return 'standard';
    }
    const value = document.documentElement.getAttribute('data-palette');
    return value === 'red-green' || value === 'blue-yellow' ? value : 'standard';
}

export function isColorBlindPalette(): boolean {
    return activePalette() !== 'standard';
}

// Maneja la accesibilidad visual. Pone en <html>:
//   data-palette="red-green | blue-yellow" (sin atributo = estándar)
//   data-contrast="high"                   (sin atributo = contraste normal)
// y styles.scss cambia los colores desde ahí.
@Injectable({ providedIn: 'root' })
export class PaletteService {
    readonly palette = signal<Palette>(this.readPalette());
    readonly highContrast = signal<boolean>(this.readContrast());

    // true si hay una opción de daltonismo activa
    readonly colorBlind = computed(() => this.palette() !== 'standard');
    // true si hay cualquier ajuste activo (para el punto del ojo)
    readonly anyActive = computed(() => this.colorBlind() || this.highContrast());

    constructor() {
        this.applyPalette(this.palette());
        this.applyContrast(this.highContrast());
    }

    // Primero se marca el <html> y luego el signal, para que las gráficas que se
    // recalculan al cambiar el signal ya lean la paleta nueva.
    set(palette: Palette): void {
        this.applyPalette(palette);
        this.palette.set(palette);
        this.save(PALETTE_KEY, palette);
    }

    setHighContrast(on: boolean): void {
        this.applyContrast(on);
        this.highContrast.set(on);
        this.save(CONTRAST_KEY, on ? 'high' : 'normal');
    }

    toggleHighContrast(): void {
        this.setHighContrast(!this.highContrast());
    }

    private readPalette(): Palette {
        try {
            const saved = localStorage.getItem(PALETTE_KEY);
            if (saved === 'red-green' || saved === 'blue-yellow') {
                return saved;
            }
            // Valores de la versión anterior del panel
            if (saved === 'protanopia' || saved === 'deuteranopia') {
                return 'red-green';
            }
        } catch {
            // Sin acceso al almacenamiento
        }
        return 'standard';
    }

    private readContrast(): boolean {
        try {
            return localStorage.getItem(CONTRAST_KEY) === 'high';
        } catch {
            return false;
        }
    }

    private save(key: string, value: string): void {
        try {
            localStorage.setItem(key, value);
        } catch {
            // Sin almacenamiento (modo privado): el ajuste solo dura mientras la pestaña está abierta
        }
    }

    private applyPalette(palette: Palette): void {
        if (typeof document === 'undefined') {
            return;
        }
        const root = document.documentElement;
        if (palette === 'standard') {
            root.removeAttribute('data-palette');
        } else {
            root.setAttribute('data-palette', palette);
        }
    }

    private applyContrast(on: boolean): void {
        if (typeof document === 'undefined') {
            return;
        }
        const root = document.documentElement;
        if (on) {
            root.setAttribute('data-contrast', 'high');
        } else {
            root.removeAttribute('data-contrast');
        }
    }
}