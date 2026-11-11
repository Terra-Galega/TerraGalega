import { Component, Input, OnChanges, OnDestroy, SimpleChanges } from '@angular/core';
import { fmtCompact, fmtMoney } from '../../../utils/dashboard-metrics';
import { niceMax, tween } from '../../../utils/tween';

// Una serie de datos: una barra/línea por cada posición del eje X.
export interface ChartSeries {
  name: string;
  values: number[];
  color: string;
  // Etiqueta larga por posición para el tooltip (ej. "3 may" del periodo anterior)
  labels?: string[];
  dashed?: boolean;
}

interface Bar { x: number; y: number; w: number; h: number; color: string; i: number }
interface Path { d: string; area: string; color: string; dashed: boolean }
interface Dot { x: number; y: number; color: string; i: number }

// Medidas internas del SVG (se escala solo al ancho de la tarjeta)
const W = 640;
const H = 260;
const PAD = { l: 52, r: 14, t: 14, b: 30 };

let uid = 0;

// Gráfica de barras o líneas con 1 o 2 series, tooltip al pasar el mouse
// y animación al entrar y cada vez que cambian los datos.
@Component({
  selector: 'app-series-chart',
  templateUrl: './series-chart.component.html',
})
export class SeriesChartComponent implements OnChanges, OnDestroy {
  @Input({ required: true }) labels: string[] = [];
  @Input({ required: true }) series: ChartSeries[] = [];
  // Título del tooltip por posición (si no se da, se usa la etiqueta del eje)
  @Input() titles: string[] = [];
  @Input() type: 'bar' | 'line' = 'bar';
  @Input() format: (n: number) => string = fmtMoney;
  @Input() axisFormat: (n: number) => string = fmtCompact;
  @Input() emptyText = 'Sin datos en este periodo';

  readonly W = W;
  readonly H = H;
  readonly padL = PAD.l;
  readonly padR = PAD.r;
  readonly gradId = `series-grad-${uid++}`;

  // Geometría ya calculada (el template solo la dibuja)
  ticks: { y: number; label: string }[] = [];
  xLabels: { x: number; text: string }[] = [];
  bars: Bar[] = [];
  paths: Path[] = [];
  dots: Dot[] = [];
  slots: { x: number; w: number }[] = [];
  hasData = false;

  // Posición bajo el mouse (null = ninguna)
  hover: number | null = null;

  private max = 1;
  private shown: number[] = [];
  private cancel: (() => void) | null = null;

  ngOnChanges(_changes?: SimpleChanges) {
    const flat = this.series.flatMap((s) => s.values);
    this.hasData = flat.some((v) => v > 0);
    this.max = niceMax(Math.max(0, ...flat));

    // Si cambió la forma de los datos se vuelve a empezar desde cero
    if (this.shown.length !== flat.length) this.shown = flat.map(() => 0);

    this.cancel?.();
    // Mismos datos de siempre: se redibuja sin animar
    if (this.shown.every((v, i) => v === flat[i])) {
      this.build();
      return;
    }
    this.cancel = tween(this.shown, flat, 700, (values) => {
      this.shown = values;
      this.build();
    });
  }

  ngOnDestroy() {
    this.cancel?.();
  }

  // Cálculo de barras, líneas, ejes y zonas de hover con los valores actuales
  private build() {
    const n = this.labels.length;
    const count = this.series.length;
    const iw = W - PAD.l - PAD.r;
    const ih = H - PAD.t - PAD.b;
    const base = PAD.t + ih;
    const slot = n ? iw / n : iw;
    const y = (v: number) => base - (v / this.max) * ih;

    this.ticks = [0, 1, 2, 3, 4].map((k) => ({
      y: y((this.max * k) / 4),
      label: this.axisFormat((this.max * k) / 4),
    }));

    // Muestra como máximo ~8 etiquetas en el eje X para que no se pisen
    const every = Math.max(1, Math.ceil(n / 8));
    this.xLabels = this.labels
      .map((text, i) => ({ x: PAD.l + slot * (i + 0.5), text, i }))
      .filter((l) => l.i % every === 0);

    this.slots = this.labels.map((_, i) => ({ x: PAD.l + slot * i, w: slot }));

    // Valores animados separados por serie
    const rows = this.series.map((s, k) =>
      this.shown.slice(k * s.values.length, (k + 1) * s.values.length),
    );

    this.bars = [];
    this.paths = [];
    this.dots = [];

    rows.forEach((row, k) => {
      const color = this.series[k].color;

      if (this.type === 'bar') {
        const group = slot * 0.72;
        const w = group / count;
        row.forEach((v, i) => {
          const h = Math.max(0, (v / this.max) * ih);
          this.bars.push({
            x: PAD.l + slot * i + (slot - group) / 2 + k * w + 1,
            y: base - h,
            w: Math.max(2, w - 2),
            h,
            color,
            i,
          });
        });
        return;
      }

      const pts = row.map((v, i) => ({ x: PAD.l + slot * (i + 0.5), y: y(v) }));
      if (!pts.length) return;
      // Curva suave que nunca se pasa de los puntos (control en la mitad)
      let d = `M ${pts[0].x} ${pts[0].y}`;
      for (let i = 1; i < pts.length; i++) {
        const mx = (pts[i - 1].x + pts[i].x) / 2;
        d += ` C ${mx} ${pts[i - 1].y}, ${mx} ${pts[i].y}, ${pts[i].x} ${pts[i].y}`;
      }
      const area = `${d} L ${pts[pts.length - 1].x} ${base} L ${pts[0].x} ${base} Z`;
      this.paths.push({ d, area, color, dashed: !!this.series[k].dashed });
      pts.forEach((p, i) => this.dots.push({ x: p.x, y: p.y, color, i }));
    });
  }

  // Color de la serie principal (degradado bajo la línea)
  get mainColor(): string {
    return this.series[0]?.color ?? '#b2571f';
  }

  // Tooltip: posición horizontal (en %) acotada para que no se salga de la tarjeta
  get tipLeft(): number {
    if (this.hover === null || !this.slots[this.hover]) return 50;
    const s = this.slots[this.hover];
    return Math.min(84, Math.max(16, ((s.x + s.w / 2) / W) * 100));
  }

  get tipTitle(): string {
    if (this.hover === null) return '';
    return this.titles[this.hover] ?? this.labels[this.hover] ?? '';
  }

  // Filas del tooltip con el valor REAL (no el animado)
  get tipRows(): { name: string; value: string; color: string; sub: string }[] {
    const i = this.hover;
    if (i === null) return [];
    return this.series.map((s) => ({
      name: s.name,
      value: this.format(s.values[i] ?? 0),
      color: s.color,
      sub: s.labels?.[i] ?? '',
    }));
  }
}