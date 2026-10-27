import { Component, Input } from '@angular/core';

// Contenedor que muestra una etiqueta encima del campo de formulario que se proyecte dentro.
@Component({
  selector: 'app-form-field',
  templateUrl: './form-field.component.html',
  styleUrl: './form-field.component.scss',
})
export class FormFieldComponent {
  // Texto de la etiqueta; si queda vacío no se muestra.
  @Input() label = '';
}