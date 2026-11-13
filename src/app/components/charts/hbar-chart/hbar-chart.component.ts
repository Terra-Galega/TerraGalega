import { Component, Input, OnChanges } from '@angular/core';
import {
  ChartConfiguration,
  ChartDataset,
  ChartType,
  Plugin,
  ScriptableContext,
} from 'chart.js';
import { BaseChartDirective } from 'ng2-charts';
import {
  INK,
  chartFont,
  tooltipStyle,
  truncate,
} from '../../../utils/chart-theme';
import { fmtMoney } from '../../../utils/dashboard-metrics';

// Un elemento del ranking (ej. un producto con sus unidades vendidas).
export interface RankItem {
  label: string;
  value: number;
  // Texto pequeño bajo el nombre (ej. la categoría)
  sub?: string;
}

// Alto de cada fila del ranking (px) y espacio extra a la derecha para el valor
const ROW_H = 44;
const ROOM = 1.38;

// Ranking de barras horizontales (Chart.js). Las barras crecen al entrar, una tras
// otra, y se ajustan con animación cuando los datos cambian.
@Component({
  selector: 'app-hbar-chart',
  imports: [BaseChartDirective],
  templateUrl: './hbar-chart.component.html',
})
export class HbarChartComponent implements OnChanges {
  @Input({ required: true }) items: RankItem[] = [];
  @Input() format: (n: number) => string = fmtMoney;
  @Input() emptyText = 'Sin ventas en este periodo';

  readonly chartType: ChartType = 'bar';
  data: ChartConfiguration['data'] = { labels: [], datasets: [] };
  options: ChartConfiguration['options'] = {};
  height = 0;

  // Escribe el valor al final de cada barra (sigue a la barra mientras crece)
  readonly plugins: Plugin[] = [
    {
      id: 'valueLabels',
      afterDatasetsDraw: (chart) => {
        const { ctx } = chart;
        ctx.save();
        const font = chartFont(12, 'bold');
        ctx.font = `${font.weight} ${font.size}px ${font.family}`;
        ctx.fillStyle = '#b2571f';
        ctx.textAlign = 'left';
        ctx.textBaseline = 'middle';
        chart.getDatasetMeta(0).data.forEach((bar, i) => {
          const item = this.items[i];
          if (!item) return;
          const { x, y } = bar.getProps(['x', 'y'], true);
          ctx.fillText(this.format(item.value), x + 8, y);
        });
        ctx.restore();
      },
    },
  ];

  ngOnChanges() {
    const values = this.items.map((i) => i.value);
    const max = Math.max(1, ...values);
    this.height = Math.max(ROW_H, this.items.length * ROW_H);

    this.data = {
      labels: this.items.map((i) => i.label),
      datasets: [
        // Barra del valor (encima)
        {
          label: 'Valor',
          data: values,
          order: 0,
          grouped: false,
          barThickness: 10,
          borderRadius: 6,
          borderSkipped: false,
          backgroundColor: (c: ScriptableContext<'bar'>) => this.fill(c),
        },
        // Pista gris de fondo (detrás)
        {
          label: 'Pista',
          data: values.map(() => max * ROOM),
          order: 1,
          grouped: false,
          barThickness: 10,
          borderRadius: 6,
          borderSkipped: false,
          backgroundColor: 'rgba(44, 44, 44, 0.07)',
        },
      ] as unknown as ChartDataset[],
    };

    this.options = {
      indexAxis: 'y',
      responsive: true,
      maintainAspectRatio: false,
      animation: {
        duration: 900,
        easing: 'easeOutQuart',
        // Las barras entran una tras otra
        delay: (c) =>
          c.type === 'data' && c.mode === 'default' ? c.dataIndex * 70 : 0,
      },
      interaction: { mode: 'index', axis: 'y', intersect: false },
      layout: { padding: { right: 4 } },
      scales: {
        x: {
          display: false,
          min: 0,
          max: max * ROOM,
          grid: { display: false },
        },
        y: {
          border: { display: false },
          grid: { display: false },
          ticks: {
            color: INK,
            font: () => chartFont(12),
            padding: 8,
            callback: (_value, index) =>
              truncate(`${index + 1}  ${this.items[index]?.label ?? ''}`, 24),
          },
        },
      },
      plugins: {
        legend: { display: false },
        tooltip: {
          ...tooltipStyle(),
          // Solo la barra del valor (no la pista gris)
          filter: (item) => item.datasetIndex === 0,
          callbacks: {
            title: (items) => this.items[items[0]?.dataIndex ?? 0]?.label ?? '',
            label: (item) => {
              const it = this.items[item.dataIndex];
              return `${this.format(it?.value ?? 0)}${it?.sub ? ' - ' + it.sub : ''}`;
            },
            labelColor: () => ({
              borderColor: '#b2571f',
              backgroundColor: '#b2571f',
              borderWidth: 0,
              borderRadius: 4,
            }),
          },
        },
      },
    };
  }

  // Degradado de cada barra: la primera en terracota→dorado, las demás en verde
  private fill(c: ScriptableContext<'bar'>): CanvasGradient | string {
    const [from, to] =
      c.dataIndex === 0 ? ['#b2571f', '#d99a21'] : ['#556141', '#8a9a6b'];
    // Chart.js entrega la barra en el contexto, aunque sus tipos no lo declaran
    const bar = (c as unknown as { element?: { x: number; base: number } })
      .element;
    if (!bar || !Number.isFinite(bar.x) || bar.x === bar.base) return from;

    const gradient = c.chart.ctx.createLinearGradient(bar.base, 0, bar.x, 0);
    gradient.addColorStop(0, from);
    gradient.addColorStop(1, to);
    return gradient;
  }
}