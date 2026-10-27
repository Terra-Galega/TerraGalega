import { Component, Input } from '@angular/core';

// Una cifra destacada: el número o texto grande y su descripción debajo.
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
  // Cifras a mostrar. Se dibujan en 3 columnas, así que está pensado para recibir 3 elementos.
  @Input({ required: true }) stats: Stat[] = [];
}
