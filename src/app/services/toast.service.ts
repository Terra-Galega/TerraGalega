import { Injectable, signal } from '@angular/core';

export interface Toast {
  title: string;
  message: string;
}

// Avisos temporales (toast) que se pueden lanzar desde cualquier pantalla
@Injectable({ providedIn: 'root' })
export class ToastService {
  readonly toast = signal<Toast | null>(null);
  private timer: ReturnType<typeof setTimeout> | null = null;

  show(title: string, message = '', durationMs = 3500) {
    if (this.timer) {
      clearTimeout(this.timer);
    }

    this.toast.set({ title, message });
    this.timer = setTimeout(() => this.close(), durationMs);
  }

  close() {
    if (this.timer) {
      clearTimeout(this.timer);
      this.timer = null;
    }

    this.toast.set(null);
  }
}