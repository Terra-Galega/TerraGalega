import { Injectable, signal } from '@angular/core';

// Define la estructura de los avisos que se muestran en la aplicación.
export interface Toast {
  title: string;
  message: string;
}

// Servicio que permite mostrar y cerrar avisos temporales desde cualquier pantalla.
@Injectable({ providedIn: 'root' })
export class ToastService {
  // Almacena el aviso actual y permite que los componentes reaccionen a sus cambios.
  readonly toast = signal<Toast | null>(null);

  // Guarda la referencia del temporizador para controlar cuándo desaparece el aviso.
  private timer: ReturnType<typeof setTimeout> | null = null;

  // Muestra un aviso durante el tiempo indicado y reemplaza cualquier aviso anterior.
  show(title: string, message = '', durationMs = 3500) {
    // Cancela el temporizador anterior para evitar que cierre el nuevo aviso.
    if (this.timer) {
      clearTimeout(this.timer);
    }

    // Actualiza el aviso visible y programa su cierre automático.
    this.toast.set({ title, message });
    this.timer = setTimeout(() => this.close(), durationMs);
  }

  // Cierra el aviso actual y cancela su temporizador si sigue activo.
  close() {
    if (this.timer) {
      clearTimeout(this.timer);
      this.timer = null;
    }

    // Indica que no hay ningún aviso visible.
    this.toast.set(null);
  }
}