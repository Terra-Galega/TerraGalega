import { Component, Input } from '@angular/core';

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
  // Tipo nativo del botón; por defecto 'button' para que no envíe formularios sin querer.
  @Input() type: 'button' | 'submit' | 'reset' = 'button';
  // Si es true, el botón se deshabilita y no responde a clics.
  @Input() disabled = false;
}