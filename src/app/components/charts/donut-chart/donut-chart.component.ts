import { DecimalPipe } from '@angular/common';
import { AfterViewInit, Component, Input, OnChanges } from '@angular/core';
import { fmtCompact, fmtMoney } from '../../../utils/dashboard-metrics';

export interface DonutItem {
  label: string;
  value: number;
  color: string;
}

interface Segment extends DonutItem {
  pct: number;
  // Largo del arco y posición donde empieza (en unidades de la circunferencia)
  len: number;
  offset: number;
}

const R = 70;
const C = 2 * Math.PI * R;
// Espacio visible entre arcos
const GAP = 3;

// Gráfica circular: los arcos se dibujan al entrar, resaltan al pasar el mouse
// (también desde la leyenda) y el centro muestra el detalle del arco activo.
@Component({
  selector: 'app-donut-chart',
  imports: [DecimalPipe],
  templateUrl: './donut-chart.component.html',
})
export class DonutChartComponent implements OnChanges, AfterViewInit {
  @Input({ required: true }) items: DonutItem[] = [];
  @Input() format: (n: number) => string = fmtMoney;
  // Cómo se escribe el total del centro (por defecto $12K; para unidades usar String)
  @Input() centerFormat: (n: number) => string = fmtCompact;
  // Texto bajo el total del centro (ej. "ventas")
  @Input() centerLabel = 'total';
  @Input() emptyText = 'Sin ventas en este periodo';

  readonly R = R;
  readonly C = C;

  segments: Segment[] = [];
  total = 0;
  active: number | null = null;
  // false al inicio: los arcos parten con largo 0 y se "dibujan"
  private animated = false;

  ngOnChanges() {
    this.build();
  }

  ngAfterViewInit() {
    setTimeout(() => {
      this.animated = true;
      this.build();
    }, 40);
  }

  private build() {
    this.total = this.items.reduce((sum, i) => sum + i.value, 0);
    let offset = 0;
    this.segments = this.items
      .filter((i) => i.value > 0)
      .map((i) => {
        const pct = this.total ? i.value / this.total : 0;
        const full = pct * C;
        const seg: Segment = {
          ...i,
          pct: pct * 100,
          len: this.animated ? Math.max(0, full - GAP) : 0,
          offset,
        };
        offset += full;
        return seg;
      });
  }

  get activeSegment(): Segment | null {
    return this.active === null ? null : (this.segments[this.active] ?? null);
  }
}