import { Component, EventEmitter, Input, Output } from '@angular/core';

// Estilos disponibles del buscador: glass (vidrio) o soft (suave)
export type SearchInputVariant = 'glass' | 'soft';

// Buscador general, el ancho se define con clases sobre <app-search-input>.
// se usa con <app-search-input class="w-48" [value]="term" (valueChange)="term = $event" />
@Component({
  selector: 'app-search-input',
  templateUrl: './search-input.component.html',
  styleUrl: './search-input.component.scss',
})
export class SearchInputComponent {
  // Texto actual del buscador (lo controla el padre)
  @Input() value = '';
  // Texto de ayuda cuando está vacío; también sirve como etiqueta de accesibilidad
  @Input() placeholder = 'Buscar…';
  // Estilo visual del buscador
  @Input() variant: SearchInputVariant = 'glass';

  // Se emite el texto cada vez que el usuario escribe
  @Output() valueChange = new EventEmitter<string>();

  // Lee lo que hay escrito y se lo avisa al padre
  onInput(event: Event): void {
    this.valueChange.emit((event.target as HTMLInputElement).value);
  }
}
