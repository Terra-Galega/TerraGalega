import { Component } from '@angular/core';
import {
  StatCardsComponent,
  Stat,
} from '../../../../components/stat-cards/stat-cards.component';

@Component({
  selector: 'app-history-section',
  imports: [StatCardsComponent],
  templateUrl: './history-section.component.html',
  styleUrl: './history-section.component.scss',
})
export class HistorySectionComponent {
  stats: Stat[] = [
    { value: '6+', label: 'Años de trayectoria' },
    { value: '150+', label: 'Platos al día' },
    { value: '4.9★', label: 'Valoración promedio' },
  ];
}
