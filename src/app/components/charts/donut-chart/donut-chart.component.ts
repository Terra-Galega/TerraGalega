import { TranslatePipe } from '../../../services/translate.pipe';
import { DecimalPipe } from '@angular/common';
import { Component, Input, OnChanges, ViewChild } from '@angular/core';
import { ChartConfiguration, ChartEvent } from 'chart.js';
import { BaseChartDirective } from 'ng2-charts';
import { fmtCompact, fmtMoney } from '../../../utils/dashboard-metrics';

export interface DonutItem {
  label: string;
  value: number;
  color: string;
}

interface Segment extends DonutItem {
  pct: number;
}

// Gráfica circular (Chart.js): los arcos se dibujan al entrar, resaltan al pasar el
// mouse (también desde la leyenda) y el centro muestra el detalle del arco activo.
@Component({
  selector: 'app-donut-chart',
  imports: [DecimalPipe, BaseChartDirective, TranslatePipe],
  templateUrl: './donut-chart.component.html',
})
export class DonutChartComponent implements OnChanges {
  @Input({ required: true }) items: DonutItem[] = [];
  @Input() format: (n: number) => string = fmtMoney;
  // Cómo se escribe el total del centro (por defecto $12K; para unidades usar String)
  @Input() centerFormat: (n: number) => string = fmtCompact;
  // Texto bajo el total del centro (ej. "ventas")
  @Input() centerLabel = 'total';
  @Input() emptyText = 'Sin ventas en este periodo';

  @ViewChild(BaseChartDirective) private chartRef?: BaseChartDirective;

  segments: Segment[] = [];
  total = 0;
  active: number | null = null;

  data: ChartConfiguration<'doughnut'>['data'] = { labels: [], datasets: [] };
  readonly options: ChartConfiguration<'doughnut'>['options'] = {
    responsive: true,
    maintainAspectRatio: false,
    cutout: '66%',
    // Espacio para que el arco resaltado "salga" sin recortarse
    layout: { padding: 8 },
    animation: { duration: 900, easing: 'easeOutQuart' },
    plugins: {
      legend: { display: false },
      // El detalle se muestra en el centro del anillo, no en un tooltip
      tooltip: { enabled: false },
    },
  };

  ngOnChanges() {
    this.total = this.items.reduce((sum, i) => sum + i.value, 0);
    this.segments = this.items
      .filter((i) => i.value > 0)
      .map((i) => ({
        ...i,
        pct: this.total ? (i.value / this.total) * 100 : 0,
      }));

    // Si cambió la cantidad de arcos, el activo ya no es válido
    if (this.active !== null && !this.segments[this.active]) this.active = null;

    this.data = {
      labels: this.segments.map((s) => s.label),
      datasets: [
        {
          data: this.segments.map((s) => s.value),
          backgroundColor: this.segments.map((s) => s.color),
          borderWidth: 0,
          // Pequeño hueco entre arcos y salto al resaltar
          spacing: 3,
          borderRadius: 4,
          hoverOffset: 8,
        },
      ],
    };
  }

  get activeSegment(): Segment | null {
    return this.active === null ? null : (this.segments[this.active] ?? null);
  }

  // El mouse pasa por un arco del anillo
  onChartHover(event: { event?: ChartEvent; active?: object[] }) {
    const first = event.active?.[0] as { index: number } | undefined;
    this.active = first ? first.index : null;
  }

  // El mouse entra (o sale) de una fila de la leyenda: se resalta su arco en el anillo
  highlight(index: number | null) {
    this.active = index;
    const chart = this.chartRef?.chart;
    if (!chart) return;
    chart.setActiveElements(index === null ? [] : [{ datasetIndex: 0, index }]);
    chart.update();
  }
}
