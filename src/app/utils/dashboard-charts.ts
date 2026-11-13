import { CHART_COLORS, fmtCompact, fmtMoney } from './dashboard-metrics';
import { niceMax } from './tween';

// =====================================================================
// Gráficas para los reportes descargables (PDF y Excel).
// Se dibujan en un <canvas> y se entregan como imagen PNG, así no dependen de
// lo que se vea en pantalla ni de ninguna librería: son idénticas para cualquier
// periodo que se elija.
// =====================================================================

// Datos que necesitan las cuatro gráficas
export interface ReportCharts {
  sales: { labels: string[]; revenue: number[]; profit: number[] };
  compare: {
    labels: string[];
    current: number[];
    previous: number[];
    currentName: string;
    previousName: string;
  };
  // Platos más vendidos (unidades)
  top: { label: string; value: number }[];
  // Reparto de ventas por categoría (ingresos)
  categories: { label: string; value: number }[];
}

export interface ChartImage {
  key: 'sales' | 'compare' | 'top' | 'categories';
  dataUrl: string;
  // Tamaño lógico (la imagen real es el doble para que se vea nítida)
  w: number;
  h: number;
}

const FONT = 'Helvetica, Arial, sans-serif';
const INK = '#2c2c2c';
const MUTED = 'rgba(44, 44, 44, 0.55)';
const GRID = 'rgba(44, 44, 44, 0.09)';
const SCALE = 2;

function canvas(w: number, h: number) {
  const el = document.createElement('canvas');
  el.width = w * SCALE;
  el.height = h * SCALE;
  const ctx = el.getContext('2d')!;
  ctx.scale(SCALE, SCALE);
  // Fondo blanco con borde suave, como las tarjetas del dashboard
  ctx.fillStyle = '#ffffff';
  ctx.fillRect(0, 0, w, h);
  ctx.strokeStyle = 'rgba(44, 44, 44, 0.12)';
  ctx.lineWidth = 1;
  ctx.strokeRect(0.5, 0.5, w - 1, h - 1);
  return { el, ctx };
}

function title(ctx: CanvasRenderingContext2D, text: string, sub: string) {
  ctx.textAlign = 'left';
  ctx.textBaseline = 'alphabetic';
  ctx.fillStyle = INK;
  ctx.font = `bold 17px ${FONT}`;
  ctx.fillText(text, 24, 34);
  ctx.fillStyle = MUTED;
  ctx.font = `12px ${FONT}`;
  ctx.fillText(sub, 24, 53);
}

function legend(
  ctx: CanvasRenderingContext2D,
  w: number,
  items: { name: string; color: string }[],
) {
  ctx.font = `12px ${FONT}`;
  ctx.textAlign = 'left';
  let x = w - 24;
  // Se dibuja de derecha a izquierda para alinear a la derecha
  for (const item of [...items].reverse()) {
    const tw = ctx.measureText(item.name).width;
    x -= tw;
    ctx.fillStyle = MUTED;
    ctx.fillText(item.name, x, 34);
    x -= 14;
    ctx.fillStyle = item.color;
    ctx.beginPath();
    ctx.arc(x + 4, 30, 4.5, 0, Math.PI * 2);
    ctx.fill();
    x -= 18;
  }
}

function empty(
  ctx: CanvasRenderingContext2D,
  w: number,
  h: number,
  text: string,
) {
  ctx.fillStyle = MUTED;
  ctx.font = `14px ${FONT}`;
  ctx.textAlign = 'center';
  ctx.fillText(text, w / 2, h / 2 + 20);
}

function cut(
  ctx: CanvasRenderingContext2D,
  text: string,
  maxWidth: number,
): string {
  if (ctx.measureText(text).width <= maxWidth) return text;
  let t = text;
  while (t.length > 1 && ctx.measureText(t + '…').width > maxWidth)
    t = t.slice(0, -1);
  return t + '…';
}

// Ejes comunes: líneas guía + valores del eje Y. Devuelve el área de dibujo.
function axes(
  ctx: CanvasRenderingContext2D,
  w: number,
  h: number,
  max: number,
) {
  const padL = 62;
  const padR = 24;
  const top = 74;
  const bottom = h - 38;
  ctx.font = `11px ${FONT}`;
  ctx.textAlign = 'right';
  for (let i = 0; i <= 4; i++) {
    const y = bottom - ((bottom - top) * i) / 4;
    ctx.strokeStyle = GRID;
    ctx.beginPath();
    ctx.moveTo(padL, y);
    ctx.lineTo(w - padR, y);
    ctx.stroke();
    ctx.fillStyle = MUTED;
    ctx.fillText(fmtCompact((max * i) / 4), padL - 8, y + 4);
  }
  return { padL, padR, top, bottom };
}

function xLabels(
  ctx: CanvasRenderingContext2D,
  labels: string[],
  xOf: (i: number) => number,
  y: number,
) {
  // Muestra como máximo ~12 etiquetas para que no se pisen
  const every = Math.max(1, Math.ceil(labels.length / 12));
  ctx.font = `11px ${FONT}`;
  ctx.textAlign = 'center';
  ctx.fillStyle = MUTED;
  labels.forEach((l, i) => {
    if (i % every === 0) ctx.fillText(l, xOf(i), y);
  });
}
// Ventas y ganancia (barras agrupadas)
function salesChart(data: ReportCharts['sales'], subtitle: string): ChartImage {
  const w = 1000;
  const h = 380;
  const { el, ctx } = canvas(w, h);
  title(ctx, 'Ventas y ganancia', subtitle);
  legend(ctx, w, [
    { name: 'Ventas', color: CHART_COLORS[0] },
    { name: 'Ganancia', color: CHART_COLORS[1] },
  ]);

  const n = data.labels.length;
  const has = data.revenue.some((v) => v > 0);
  const max = niceMax(Math.max(0, ...data.revenue, ...data.profit));
  const a = axes(ctx, w, h, max);

  if (!has || n === 0) {
    empty(ctx, w, h, 'Sin ventas en este periodo');
    return { key: 'sales', dataUrl: el.toDataURL('image/png'), w, h };
  }

  const slot = (w - a.padL - a.padR) / n;
  const barW = Math.min(26, slot * 0.36);
  const hOf = (v: number) => ((a.bottom - a.top) * v) / max;
  const bar = (x: number, v: number, color: string) => {
    const bh = hOf(v);
    if (bh <= 0) return;
    ctx.fillStyle = color;
    ctx.beginPath();
    ctx.roundRect(x, a.bottom - bh, barW, bh, [3, 3, 0, 0]);
    ctx.fill();
  };

  for (let i = 0; i < n; i++) {
    const cx = a.padL + slot * i + slot / 2;
    bar(cx - barW - 1, data.revenue[i], CHART_COLORS[0]);
    bar(cx + 1, data.profit[i], CHART_COLORS[1]);
  }
  xLabels(ctx, data.labels, (i) => a.padL + slot * i + slot / 2, h - 14);
  return { key: 'sales', dataUrl: el.toDataURL('image/png'), w, h };
}
// Comparación con el periodo anterior (líneas)
function compareChart(
  data: ReportCharts['compare'],
  subtitle: string,
): ChartImage {
  const w = 1000;
  const h = 380;
  const { el, ctx } = canvas(w, h);
  title(ctx, 'Comparación de ingresos', subtitle);
  legend(ctx, w, [
    { name: data.currentName, color: CHART_COLORS[0] },
    { name: data.previousName, color: CHART_COLORS[5] },
  ]);

  const n = data.labels.length;
  const has = [...data.current, ...data.previous].some((v) => v > 0);
  const max = niceMax(Math.max(0, ...data.current, ...data.previous));
  const a = axes(ctx, w, h, max);

  if (!has || n === 0) {
    empty(ctx, w, h, 'Sin ingresos en ninguno de los dos periodos');
    return { key: 'compare', dataUrl: el.toDataURL('image/png'), w, h };
  }

  const slot = (w - a.padL - a.padR) / n;
  const xOf = (i: number) => a.padL + slot * i + slot / 2;
  const yOf = (v: number) => a.bottom - ((a.bottom - a.top) * v) / max;

  const path = (values: number[]) => {
    ctx.beginPath();
    values.forEach((v, i) =>
      i === 0 ? ctx.moveTo(xOf(i), yOf(v)) : ctx.lineTo(xOf(i), yOf(v)),
    );
  };

  // Periodo anterior: línea punteada
  ctx.setLineDash([7, 5]);
  ctx.strokeStyle = CHART_COLORS[5];
  ctx.lineWidth = 2.5;
  ctx.lineJoin = 'round';
  path(data.previous);
  ctx.stroke();
  ctx.setLineDash([]);

  // Periodo actual: área suave + línea + puntos
  const grad = ctx.createLinearGradient(0, a.top, 0, a.bottom);
  grad.addColorStop(0, 'rgba(178, 87, 31, 0.28)');
  grad.addColorStop(1, 'rgba(178, 87, 31, 0)');
  path(data.current);
  ctx.lineTo(xOf(n - 1), a.bottom);
  ctx.lineTo(xOf(0), a.bottom);
  ctx.closePath();
  ctx.fillStyle = grad;
  ctx.fill();

  ctx.strokeStyle = CHART_COLORS[0];
  ctx.lineWidth = 3;
  path(data.current);
  ctx.stroke();
  if (n <= 31) {
    ctx.fillStyle = CHART_COLORS[0];
    data.current.forEach((v, i) => {
      ctx.beginPath();
      ctx.arc(xOf(i), yOf(v), 3.5, 0, Math.PI * 2);
      ctx.fill();
    });
  }

  xLabels(ctx, data.labels, xOf, h - 14);
  return { key: 'compare', dataUrl: el.toDataURL('image/png'), w, h };
}
// Platos más vendidos (barras horizontales)
function topChart(items: ReportCharts['top'], subtitle: string): ChartImage {
  const w = 560;
  const rowH = 40;
  const h = Math.max(220, 82 + Math.max(items.length, 1) * rowH);
  const { ctx, el } = canvas(w, h);
  title(ctx, 'Productos más vendidos', subtitle);

  if (items.length === 0) {
    empty(ctx, w, h, 'Sin ventas en este periodo');
    return { key: 'top', dataUrl: el.toDataURL('image/png'), w, h };
  }

  const max = Math.max(...items.map((i) => i.value));
  items.forEach((item, i) => {
    const y = 76 + i * rowH;
    ctx.textAlign = 'left';
    ctx.font = `13px ${FONT}`;
    ctx.fillStyle = INK;
    ctx.fillText(cut(ctx, `${i + 1}. ${item.label}`, 360), 24, y + 12);
    ctx.textAlign = 'right';
    ctx.font = `bold 13px ${FONT}`;
    ctx.fillStyle = CHART_COLORS[0];
    ctx.fillText(`${Math.round(item.value)} uds`, w - 24, y + 12);

    // Pista gris y barra
    ctx.fillStyle = 'rgba(44, 44, 44, 0.07)';
    ctx.beginPath();
    ctx.roundRect(24, y + 19, w - 48, 9, 5);
    ctx.fill();
    ctx.fillStyle = i === 0 ? CHART_COLORS[0] : CHART_COLORS[1];
    ctx.beginPath();
    ctx.roundRect(24, y + 19, Math.max(9, ((w - 48) * item.value) / max), 9, 5);
    ctx.fill();
  });
  return { key: 'top', dataUrl: el.toDataURL('image/png'), w, h };
}
// Ventas por categoría (circular)
function categoryChart(
  items: ReportCharts['categories'],
  subtitle: string,
): ChartImage {
  const w = 560;
  const h = Math.max(300, 100 + items.length * 26 + 20);
  const { ctx, el } = canvas(w, h);
  title(ctx, 'Ventas por categoría', subtitle);

  const total = items.reduce((s, i) => s + i.value, 0);
  if (items.length === 0 || total <= 0) {
    empty(ctx, w, h, 'Sin ventas en este periodo');
    return { key: 'categories', dataUrl: el.toDataURL('image/png'), w, h };
  }

  // Anillo
  const cx = 24 + 90;
  const cy = 74 + 100;
  const r = 82;
  ctx.lineWidth = 30;
  ctx.lineCap = 'butt';
  let angle = -Math.PI / 2;
  items.forEach((item, i) => {
    const sweep = (item.value / total) * Math.PI * 2;
    ctx.strokeStyle = CHART_COLORS[i % CHART_COLORS.length];
    ctx.beginPath();
    // Un pequeño hueco entre arcos cuando hay más de uno
    const gap = items.length > 1 ? 0.02 : 0;
    ctx.arc(cx, cy, r, angle + gap, angle + sweep - gap);
    ctx.stroke();
    angle += sweep;
  });

  // Total en el centro
  ctx.textAlign = 'center';
  ctx.fillStyle = INK;
  ctx.font = `bold 20px ${FONT}`;
  ctx.fillText(fmtCompact(total), cx, cy + 4);
  ctx.fillStyle = MUTED;
  ctx.font = `11px ${FONT}`;
  ctx.fillText('ventas', cx, cy + 22);

  // Leyenda
  items.forEach((item, i) => {
    const y = 90 + i * 26;
    const x = 230;
    ctx.fillStyle = CHART_COLORS[i % CHART_COLORS.length];
    ctx.beginPath();
    ctx.arc(x, y, 5, 0, Math.PI * 2);
    ctx.fill();

    ctx.textAlign = 'left';
    ctx.font = `12px ${FONT}`;
    ctx.fillStyle = INK;
    ctx.fillText(cut(ctx, item.label, 100), x + 14, y + 4);
    ctx.fillStyle = MUTED;
    ctx.fillText(`${Math.round((item.value / total) * 100)}%`, x + 122, y + 4);
    ctx.textAlign = 'right';
    ctx.font = `bold 12px ${FONT}`;
    ctx.fillStyle = INK;
    ctx.fillText(fmtMoney(item.value), w - 24, y + 4);
  });
  return { key: 'categories', dataUrl: el.toDataURL('image/png'), w, h };
}

// Dibuja las cuatro gráficas del reporte
export function renderCharts(
  data: ReportCharts,
  periodText: string,
): ChartImage[] {
  return [
    salesChart(data.sales, periodText),
    compareChart(
      data.compare,
      `${data.compare.currentName} frente a ${data.compare.previousName}`,
    ),
    topChart(data.top, periodText),
    categoryChart(data.categories, periodText),
  ];
}
