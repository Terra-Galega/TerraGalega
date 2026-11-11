import { AfterViewInit, Component, Input } from '@angular/core';
import { fmtMoney } from '../../../utils/dashboard-metrics';

// Un elemento del ranking (ej. un producto con sus unidades vendidas).
export interface RankItem {
  label: string;
  value: number;
  // Texto pequeño bajo el nombre (ej. la categoría)
  sub?: string;
}

// Ranking de barras horizontales. Las barras crecen al entrar y se
// reajustan con una transición CSS cuando los datos cambian.
@Component({
  selector: 'app-hbar-chart',
  templateUrl: './hbar-chart.component.html',
})
export class HbarChartComponent implements AfterViewInit {
  @Input({ required: true }) items: RankItem[] = [];
  @Input() format: (n: number) => string = fmtMoney;
  @Input() emptyText = 'Sin ventas en este periodo';

  // false al inicio para que las barras partan de 0 y "crezcan"
  animated = false;
  hover: number | null = null;

  ngAfterViewInit() {
    setTimeout(() => (this.animated = true), 40);
  }

  get max(): number {
    return Math.max(1, ...this.items.map((i) => i.value));
  }

  width(item: RankItem): number {
    return this.animated ? (item.value / this.max) * 100 : 0;
  }
}
