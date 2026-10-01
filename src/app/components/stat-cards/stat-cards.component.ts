import { Component, Input } from '@angular/core';

export interface Stat {
  value: string;
  label: string;
}

// Fila de tarjetas con cifras destacadas (ej. "6+ Años de trayectoria").
// Recibe los datos por @Input, así cualquier página puede usarla.
@Component({
  selector: 'app-stat-cards',
  templateUrl: './stat-cards.component.html',
  styleUrl: './stat-cards.component.scss',
})
export class StatCardsComponent {
  @Input({ required: true }) stats: Stat[] = [];
}
