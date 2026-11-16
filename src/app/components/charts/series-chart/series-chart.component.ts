import { TranslatePipe } from '../../../services/translate.pipe';
import { Component, Input, OnChanges } from '@angular/core';
import { ChartConfiguration, ChartDataset, ChartType, Plugin } from 'chart.js';
import { BaseChartDirective } from 'ng2-charts';
import {
  areaGradient,
  chartFont,
  gridColor,
  isDarkTheme,
  tickColor,
  tooltipStyle,
} from '../../../utils/chart-theme';
import { fmtCompact, fmtMoney } from '../../../utils/dashboard-metrics';

// Una serie de datos: una barra/línea por cada posición del eje X.
export interface ChartSeries {
  name: string;
  values: number[];
  color: string;
  // Etiqueta larga por posición para el tooltip (ej. "3 may" del periodo anterior)
  labels?: string[];
  dashed?: boolean;
}

// Franja suave detrás de la posición bajo el mouse
const hoverBand: Plugin = {
  id: 'hoverBand',
  beforeDatasetsDraw(chart) {
    const active = chart.tooltip?.getActiveElements();
    const n = chart.data.labels?.length ?? 0;
    if (!active?.length || !n) return;

    const { ctx, chartArea } = chart;
    const slot = chartArea.width / n;
    ctx.save();
    ctx.fillStyle = 'rgba(178, 87, 31, 0.07)';
    ctx.beginPath();
    ctx.roundRect(
      chartArea.left + slot * active[0].index,
      chartArea.top,
      slot,
      chartArea.height,
      6,
    );
    ctx.fill();
    ctx.restore();
  },
};

// Gráfica de barras o líneas con 1 o 2 series (Chart.js), con tooltip al pasar
// el mouse y animación al entrar y cada vez que cambian los datos.
@Component({
  selector: 'app-series-chart',
  imports: [BaseChartDirective, TranslatePipe],
  templateUrl: './series-chart.component.html',
})
export class SeriesChartComponent implements OnChanges {
  @Input({ required: true }) labels: string[] = [];
  @Input({ required: true }) series: ChartSeries[] = [];
  // Título del tooltip por posición (si no se da, se usa la etiqueta del eje)
  @Input() titles: string[] = [];
  @Input() type: 'bar' | 'line' = 'bar';
  @Input() format: (n: number) => string = fmtMoney;
  @Input() axisFormat: (n: number) => string = fmtCompact;
  @Input() emptyText = 'Sin datos en este periodo';

  // Lo que recibe Chart.js (se recalcula cuando cambian los @Input)
  chartType: ChartType = 'bar';
  data: ChartConfiguration['data'] = { labels: [], datasets: [] };
  options: ChartConfiguration['options'] = {};
  readonly plugins: Plugin[] = [hoverBand];
  hasData = false;

  ngOnChanges() {
    this.hasData = this.series.some((s) => s.values.some((v) => v > 0));
    this.chartType = this.type;
    this.data = {
      labels: this.labels,
      datasets: this.series.map((s, k) => this.dataset(s, k)),
    };
    this.options = this.buildOptions();
  }

  private dataset(s: ChartSeries, k: number): ChartDataset {
    if (this.type === 'bar') {
      return {
        label: s.name,
        data: s.values,
        backgroundColor: s.color,
        borderRadius: 3,
        borderSkipped: 'bottom',
        categoryPercentage: 0.72,
        barPercentage: 0.95,
        maxBarThickness: 28,
      } as unknown as ChartDataset;
    }

    // Línea: solo la primera serie lleva el área con degradado; las demás pueden ir punteadas
    return {
      label: s.name,
      data: s.values,
      borderColor: s.color,
      borderWidth: 2.5,
      borderCapStyle: 'round',
      borderJoinStyle: 'round',
      borderDash: s.dashed ? [6, 5] : [],
      // Curva suave que nunca se pasa de los puntos
      cubicInterpolationMode: 'monotone',
      pointRadius: 0,
      pointHoverRadius: 5,
      pointHoverBackgroundColor: isDarkTheme() ? '#262320' : '#ffffff',
      pointHoverBorderColor: s.color,
      pointHoverBorderWidth: 2.5,
      pointHitRadius: 20,
      fill: k === 0 ? 'origin' : false,
      backgroundColor: k === 0 ? areaGradient(s.color) : s.color,
    } as unknown as ChartDataset;
  }

  private buildOptions(): ChartConfiguration['options'] {
    return {
      responsive: true,
      maintainAspectRatio: false,
      animation: { duration: 700, easing: 'easeOutCubic' },
      // El tooltip muestra TODAS las series de la posición, no solo la barra tocada
      interaction: { mode: 'index', intersect: false },
      layout: { padding: { top: 8, right: 4 } },
      scales: {
        x: {
          offset: true,
          grid: { display: false },
          border: { display: false },
          ticks: {
            color: tickColor,
            font: () => chartFont(10),
            autoSkip: true,
            maxTicksLimit: 8,
            maxRotation: 0,
          },
        },
        y: {
          beginAtZero: true,
          border: { display: false },
          grid: { color: gridColor },
          ticks: {
            color: tickColor,
            font: () => chartFont(10),
            maxTicksLimit: 5,
            padding: 8,
            callback: (value) => this.axisFormat(Number(value)),
          },
        },
      },
      plugins: {
        legend: { display: false },
        tooltip: {
          ...tooltipStyle(),
          callbacks: {
            title: (items) => {
              const i = items[0]?.dataIndex ?? 0;
              return this.titles[i] ?? this.labels[i] ?? '';
            },
            label: (item) => {
              const s = this.series[item.datasetIndex];
              const sub = s?.labels?.[item.dataIndex];
              const value = this.format(Number(item.raw));
              return `${s?.name ?? ''}${sub ? ' - ' + sub : ''}:  ${value}`;
            },
            labelColor: (item) => {
              const color = this.series[item.datasetIndex]?.color ?? '#b2571f';
              return {
                borderColor: color,
                backgroundColor: color,
                borderWidth: 0,
                borderRadius: 4,
              };
            },
          },
        },
      },
    };
  }
}
