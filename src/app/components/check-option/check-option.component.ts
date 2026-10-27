import { Component, EventEmitter, Input, Output } from '@angular/core';

// Casilla de verificación con texto. Se usa en los modales del admin y en los
// filtros del menú. Avisa al padre cuando se marca o se desmarca.
@Component({
  selector: 'app-check-option',
  templateUrl: './check-option.component.html',
  styleUrl: './check-option.component.scss',
})
export class CheckOptionComponent {
  // Texto que acompaña a la casilla
  @Input() label = '';
  // Si está marcada o no (lo controla el padre)
  @Input() checked = false;
  // terra: admin (naranja) · sage: filtros del menú (verde, con icono opcional dentro)
  @Input() accent: 'terra' | 'sage' = 'terra';

  // Se emite true o false cada vez que el usuario cambia la casilla
  @Output() changed = new EventEmitter<boolean>();

  // Lee el estado de la casilla y se lo avisa al padre
  onChange(event: Event) {
    this.changed.emit((event.target as HTMLInputElement).checked);
  }
}
