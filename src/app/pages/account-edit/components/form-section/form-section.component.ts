import { Component, Input } from '@angular/core';

@Component({
  selector: 'app-form-section',
  templateUrl: './form-section.component.html',
  host: { class: 'block' },
})
export class FormSectionComponent {
  // Título que se muestra en el encabezado de la sección del formulario
  @Input() title = '';
}