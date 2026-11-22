import { Component, Input, OnChanges } from '@angular/core';
import {
  DonutChartComponent,
  DonutItem,
} from '../../../../../../components/charts/donut-chart/donut-chart.component';
import {
  ChannelStats,
  chartColors,
  fmtCompact,
  fmtMoney,
} from '../../../../../../utils/dashboard-metrics';

// Tarjeta del dashboard: ventas a domicilio frente a las del punto físico
// (meseros), con el cobro del salón y los meseros que más venden.
@Component({
  selector: 'app-dashboard-channels',
  imports: [DonutChartComponent],
  templateUrl: './dashboard-channels.component.html',
})
export class DashboardChannelsComponent implements OnChanges {
  @Input({ required: true }) stats!: ChannelStats;
  @Input() rangeLabel = '';

  readonly money = fmtMoney;
  readonly compact = fmtCompact;

  // Se calcula solo cuando cambian los datos (la gráfica no se redibuja en cada ciclo)
  split: DonutItem[] = [];

  ngOnChanges() {
    const palette = chartColors();
    this.split = [
      { label: 'Domicilio', value: this.stats.delivery.revenue, color: palette[0] },
      { label: 'Punto físico', value: this.stats.table.revenue, color: palette[1] },
    ];
  }

  get topWaiters() {
    return this.stats.waiters.slice(0, 5);
  }
}
