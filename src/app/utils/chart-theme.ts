// Estilo común de las gráficas (Chart.js): colores, tipografía y tooltip
// de Terra Galega. Los tres componentes de gráficas lo comparten para que
// se vean como un mismo sistema.

import { ChartOptions, ScriptableContext } from 'chart.js';

export const INK = '#2c2c2c';
export const GRID_COLOR = 'rgba(44, 44, 44, 0.08)';
export const TICK_COLOR = 'rgba(44, 44, 44, 0.5)';

type TooltipOptions = NonNullable<
  NonNullable<ChartOptions['plugins']>['tooltip']
>;

// Factor de tamaño de texto del dashboard según la pantalla (--dash-k en styles.scss)
export function dashScale(): number {
  if (typeof document === 'undefined') return 1;
  const k = parseFloat(
    getComputedStyle(document.documentElement).getPropertyValue('--dash-k'),
  );
  return Number.isFinite(k) && k > 0 ? k : 1;
}

// Misma tipografía que el resto de la app (la hereda del <body>), escalada
// con el mismo factor que el texto del dashboard
export function chartFont(size = 11, weight: 'normal' | 'bold' = 'normal') {
  const family =
    typeof document !== 'undefined'
      ? getComputedStyle(document.body).fontFamily
      : 'sans-serif';
  return { family, size: Math.round(size * dashScale() * 10) / 10, weight };
}

// "#b2571f" + 0.28 → "rgba(178, 87, 31, 0.28)"
export function rgba(hex: string, alpha: number): string {
  const h = hex.replace('#', '');
  const full = h.length === 3 ? h.replace(/(.)/g, '$1$1') : h;
  const n = parseInt(full, 16);
  return `rgba(${(n >> 16) & 255}, ${(n >> 8) & 255}, ${n & 255}, ${alpha})`;
}

// Recorta un texto largo con "…" (para etiquetas de ejes)
export function truncate(text: string, max: number): string {
  return text.length > max ? text.slice(0, max - 1).trimEnd() + '…' : text;
}

// Tooltip oscuro redondeado, como el del dashboard original
export function tooltipStyle(): TooltipOptions {
  return {
    backgroundColor: INK,
    titleColor: '#f5f2eb',
    bodyColor: 'rgba(245, 242, 235, 0.85)',
    titleFont: () => chartFont(12, 'bold'),
    bodyFont: () => chartFont(12),
    padding: { top: 8, bottom: 8, left: 12, right: 12 },
    cornerRadius: 12,
    boxWidth: 8,
    boxHeight: 8,
    boxPadding: 4,
    caretSize: 0,
    displayColors: true,
  };
}

// Degradado vertical bajo una línea: del color (28 %) a transparente
export function areaGradient(color: string) {
  return (context: ScriptableContext<'line'>) => {
    const { ctx, chartArea } = context.chart;
    // Al crearse la gráfica todavía no hay área dibujable
    if (!chartArea) return rgba(color, 0);
    const gradient = ctx.createLinearGradient(
      0,
      chartArea.top,
      0,
      chartArea.bottom,
    );
    gradient.addColorStop(0, rgba(color, 0.28));
    gradient.addColorStop(1, rgba(color, 0));
    return gradient;
  };
}
