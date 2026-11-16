import { Injectable, signal } from '@angular/core';
import es from '../i18n/es.json';
import en from '../i18n/en.json';
import ca from '../i18n/ca.json';

export type Language = 'es' | 'en' | 'ca';

const dictionaries: Record<Language, Record<string, string>> = { es, en, ca };
const sourceToKey = new Map(Object.entries(es).map(([key, value]) => [value, key]));
const languageOrder: Language[] = ['es', 'en', 'ca'];
const storageKey = 'terragalega_language';

@Injectable({ providedIn: 'root' })
export class LanguageService {
  readonly language = signal<Language>(this.read());

  constructor() {
    document.documentElement.lang = this.language();
  }

  t(key: string): string {
    const resolvedKey = sourceToKey.get(key) ?? key;
    return dictionaries[this.language()][resolvedKey] ?? dictionaries.es[resolvedKey] ?? key;
  }

  setLanguage(next: Language): void {
    this.language.set(next);
    document.documentElement.lang = next;
    try {
      localStorage.setItem(storageKey, next);
    } catch {
      // El idioma sigue activo durante esta sesión si no se puede guardar.
    }
  }

  toggle(): void {
    this.setLanguage(this.nextLanguage());
  }

  nextLanguage(): Language {
    const index = languageOrder.indexOf(this.language());
    return languageOrder[(index + 1) % languageOrder.length];
  }

  private read(): Language {
    try {
      const saved = localStorage.getItem(storageKey);
      return saved === 'en' || saved === 'ca' ? saved : 'es';
    } catch {
      return 'es';
    }
  }
}
