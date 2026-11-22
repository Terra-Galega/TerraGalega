import { inject, Injectable, signal } from '@angular/core';
import { LanguageService } from './language.service';

// Define la estructura de los avisos que se muestran en la aplicación.
export interface Toast {
  title: string;
  message: string;
  // success: acción completada (icono de check verde); error: algo falló (icono rojo)
  type: 'success' | 'error';
  // Botón opcional dentro del aviso, p. ej. "Ver carrito" después de añadir un plato
  action?: ToastAction;
}

export interface ToastAction {
  label: string;
  run: () => void;
}

// Servicio que permite mostrar y cerrar avisos temporales desde cualquier pantalla.
@Injectable({ providedIn: 'root' })
export class ToastService {
  private readonly language = inject(LanguageService);
  // Almacena el aviso actual y permite que los componentes reaccionen a sus cambios.
  readonly toast = signal<Toast | null>(null);

  // Guarda la referencia del temporizador para controlar cuándo desaparece el aviso.
  private timer: ReturnType<typeof setTimeout> | null = null;

  // Muestra un aviso durante el tiempo indicado y reemplaza cualquier aviso anterior.
  show(
    title: string,
    message = '',
    durationMs = 3500,
    type: Toast['type'] = 'success',
    action?: ToastAction,
  ) {
    // Cancela el temporizador anterior para evitar que cierre el nuevo aviso.
    if (this.timer) {
      clearTimeout(this.timer);
    }

    // Actualiza el aviso visible y programa su cierre automático.
    // Siempre es un objeto nuevo: así el aviso se vuelve a animar aunque ya hubiera otro visible.
    this.toast.set({
      title: this.language.t(title),
      message: this.language.t(message),
      type,
      action: action ? { label: this.language.t(action.label), run: action.run } : undefined,
    });
    this.timer = setTimeout(() => this.close(), durationMs);
  }

  // Aviso de error (no se pudo completar la acción)
  error(title: string, message = '', durationMs = 4500) {
    this.show(title, message, durationMs, 'error');
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
