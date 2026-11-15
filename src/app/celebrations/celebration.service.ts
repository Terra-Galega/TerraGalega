import { Injectable, computed, signal } from '@angular/core';
import { CELEBRATIONS, Celebration } from './celebrations.data';

const PREVIEW_KEY = 'terra-galega-celebration-preview';

// Día de hoy en Bogotá, como MM-DD
function todayInBogota(now = new Date()): string {
  const parts = new Intl.DateTimeFormat('en-US', {
    timeZone: 'America/Bogota',
    month: '2-digit',
    day: '2-digit',
  }).formatToParts(now);
  const month = parts.find((part) => part.type === 'month')?.value;
  const day = parts.find((part) => part.type === 'day')?.value;
  return `${month}-${day}`;
}

function readPreview(): string | null {
  try {
    return sessionStorage.getItem(PREVIEW_KEY);
  } catch {
    return null;
  }
}

// Decide qué festividad se muestra: la del día en Bogotá o la que se elija en la vista previa.
@Injectable({ providedIn: 'root' })
export class CelebrationService {
  // null: usa la fecha real · 'none': sin efecto · un id: fuerza esa festividad
  readonly preview = signal<string | null>(readPreview());

  private readonly today = signal(todayInBogota());

  // Festividad a mostrar ahora (null si no hay ninguna)
  readonly current = computed<Celebration | null>(() => {
    const preview = this.preview();
    if (preview === 'none') return null;
    return (
      CELEBRATIONS.find((item) => item.id === preview) ??
      CELEBRATIONS.find((item) => item.date === this.today()) ??
      null
    );
  });

  // En la vista previa las figuras se mueven aunque el usuario pida menos animaciones
  readonly forceMotion = computed(() => {
    const preview = this.preview();
    return !!preview && preview !== 'none';
  });

  constructor() {
    // Si la página queda abierta pasada la medianoche, cambia de festividad sola
    setInterval(() => this.today.set(todayInBogota()), 60_000);
  }

  // '' vuelve a la fecha real
  setPreview(id: string): void {
    this.preview.set(id || null);
    try {
      if (id) sessionStorage.setItem(PREVIEW_KEY, id);
      else sessionStorage.removeItem(PREVIEW_KEY);
    } catch {
      // Sin sessionStorage la vista previa solo dura mientras no se recargue
    }
  }
}
