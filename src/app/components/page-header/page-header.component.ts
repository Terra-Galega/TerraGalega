import { Component, Input } from '@angular/core';

// Cabecera de página con un texto pequeño superior (eyebrow) y un título principal.
@Component({
  selector: 'app-page-header',
  templateUrl: './page-header.component.html',
  styleUrl: './page-header.component.scss',
})
export class PageHeaderComponent {
  // Texto pequeño en mayúsculas que se muestra encima del título.
  @Input() eyebrow = '';
  // Título principal de la página.
  @Input() title = '';

  // left: título a la izquierda y contenido proyectado a la derecha (menu)
  // center: título centrado con línea dorada debajo (contacto)
  @Input() align: 'left' | 'center' = 'left';
}
