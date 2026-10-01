import { Component, Input } from '@angular/core';

@Component({
  selector: 'app-page-header',
  templateUrl: './page-header.component.html',
  styleUrl: './page-header.component.scss',
})
export class PageHeaderComponent {
  @Input() eyebrow = '';
  @Input() title = '';

  // left: título a la izquierda y contenido proyectado a la derecha (menu)
  // center: título centrado con línea dorada debajo (contacto)
  @Input() align: 'left' | 'center' = 'left';
}
