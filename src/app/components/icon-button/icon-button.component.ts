import { Component, Input, inject } from '@angular/core';
import { BusyService } from '../../services/busy.service';

// Variantes visuales permitidas: 'soft' tiene fondo suave y 'ghost' es transparente hasta pasar el mouse.
export type IconButtonVariant = 'soft' | 'ghost';

@Component({
  selector: 'app-icon-button',
  templateUrl: './icon-button.component.html',
  styleUrl: './icon-button.component.scss',
})
export class IconButtonComponent {
  // Estilo visual del botón; por defecto 'soft'.
  @Input() variant: IconButtonVariant = 'soft';
  // Texto accesible que describe la acción, ya que el botón solo muestra un icono.
  @Input() label = '';
  // Cerrar o eliminar: al pasar el mouse se pone rojo, como el botón "Cerrar sesión"
  @Input() danger = false;
  // Tipo nativo del botón; por defecto 'button' para que no envíe formularios sin querer.
  @Input() type: 'button' | 'submit' | 'reset' = 'button';
  // Si es true, el botón se deshabilita y no responde a clics.
  @Input() disabled = false;
  // Si es true, el botón sigue activo mientras se guarda algo.
  @Input() ignoreBusy = false;

  // Mientras haya una petición que crea, edita o borra, el botón también se deshabilita
  protected busy = inject(BusyService);

  get isDisabled(): boolean {
    return this.disabled || (!this.ignoreBusy && this.busy.busyVisible());
  }
}