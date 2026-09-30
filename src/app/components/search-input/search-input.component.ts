import { Component, EventEmitter, Input, Output } from '@angular/core';

export type SearchInputVariant = 'glass' | 'soft';

// Buscador general, el ancho se define con clases sobre <app-search-input>.
// se usa con <app-search-input class="w-48" [value]="term" (valueChange)="term = $event" />
@Component({
  selector: 'app-search-input',
  templateUrl: './search-input.component.html',
  styleUrl: './search-input.component.scss',
})
export class SearchInputComponent {
  @Input() value = '';
  @Input() placeholder = 'Buscar…';
  @Input() variant: SearchInputVariant = 'glass';

  @Output() valueChange = new EventEmitter<string>();

  onInput(event: Event): void {
    this.valueChange.emit((event.target as HTMLInputElement).value);
  }
}