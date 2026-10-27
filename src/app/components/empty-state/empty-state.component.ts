import { Component, EventEmitter, Input, Output } from '@angular/core';

// Mensaje para cuando una lista no tiene nada que mostrar (sin resultados,
// sin pedidos, etc.). Opcionalmente muestra un enlace de acción, por ejemplo
// "Explorar la carta". La página que lo usa decide qué hace esa acción.
@Component({
  selector: 'app-empty-state',
  templateUrl: './empty-state.component.html',
  styleUrl: './empty-state.component.scss',
})
export class EmptyStateComponent {
  // Texto principal que se muestra al cliente
  @Input() message = '';
  // Texto del botón de acción. Si está vacío, el botón no se muestra.
  @Input() actionLabel = '';
  // Sirve para usarlo dentro de tarjetas o tablas.
  @Input() compact = false;

  // Se emite cuando el cliente pulsa el botón de acción
  @Output() action = new EventEmitter<void>();
}