import { Component, Input } from '@angular/core';

// Encabezado centrado de una sección: texto pequeño arriba y título grande.
// Se reutiliza en las secciones del inicio y en Nosotros.
@Component({
  selector: 'app-section-heading',
  templateUrl: './section-heading.component.html',
  styleUrl: './section-heading.component.scss',
})
export class SectionHeadingComponent {
  @Input() eyebrow = ''; // texto pequeño superior
  @Input() title = '';
}
