import { Component } from '@angular/core';
import { RouterLink } from '@angular/router';

/**
 * Pie de página del sitio: logo, datos de contacto (dirección, correo,
 * teléfono y horario), redes sociales y copyright. Se usa al final de las
 * páginas públicas.
 */
@Component({
  selector: 'app-footer',
  standalone: true,
  // RouterLink permite que el logo lleve al inicio sin recargar la página
  imports: [RouterLink],
  templateUrl: './footer.component.html',
  styleUrl: './footer.component.scss',
})
export class FooterComponent {
  // Año actual para el copyright. Se calcula solo, así no hay que
  // actualizarlo a mano cada año (antes estaba escrito fijo: © 2026).
  readonly currentYear = new Date().getFullYear();
}
