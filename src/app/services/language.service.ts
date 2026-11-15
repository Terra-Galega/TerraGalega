import { Injectable, signal } from '@angular/core';

export type Language = 'es' | 'en';

const translations = {
    es: {
        settings: 'Configuración',
        closeSettings: 'Cerrar configuración',
        theme: 'Tema',
        light: 'Claro',
        dark: 'Oscuro',
        language: 'Idioma',
        spanish: 'Español',
        english: 'English',
        about: 'NOSOTROS',
        menu: 'MENÚ',
        reservations: 'RESERVAS',
        orders: 'PEDIDOS',
        myOrders: 'MIS PEDIDOS',
        signIn: 'INICIAR SESIÓN',
        signOut: 'CERRAR SESIÓN',
        notifications: 'Notificaciones',
        cart: 'Carrito',
    },
    en: {
        settings: 'Settings',
        closeSettings: 'Close settings',
        theme: 'Theme',
        light: 'Light',
        dark: 'Dark',
        language: 'Language',
        spanish: 'Español',
        english: 'English',
        about: 'ABOUT US',
        menu: 'MENU',
        reservations: 'RESERVATIONS',
        orders: 'ORDERS',
        myOrders: 'MY ORDERS',
        signIn: 'SIGN IN',
        signOut: 'SIGN OUT',
        notifications: 'Notifications',
        cart: 'Cart',
    },
} as const;

type TranslationKey = keyof typeof translations.es;

@Injectable({ providedIn: 'root' })
export class LanguageService {
    readonly language = signal<Language>(this.read());

    constructor() {
        document.documentElement.lang = this.language();
    }

    t(key: TranslationKey): string {
        return translations[this.language()][key];
    }

    toggle(): void {
        const next: Language = this.language() === 'es' ? 'en' : 'es';
        this.language.set(next);
        document.documentElement.lang = next;

        try {
            localStorage.setItem('terragalega_language', next);
        } catch {
            // La selección sigue activa hasta cerrar la pestaña.
        }
    }

    private read(): Language {
        try {
            return localStorage.getItem('terragalega_language') === 'en' ? 'en' : 'es';
        } catch {
            return 'es';
        }
    }
}