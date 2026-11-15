// Estilo común de las gráficas (Chart.js): colores, tipografía y tooltip
// de Terra Galega. Los tres componentes de gráficas lo comparten para que
// se vean como un mismo sistema.

import { ChartOptions, ScriptableContext } from 'chart.js';

export const INK = '#2c2c2c';
export const GRID_COLOR = 'rgba(44, 44, 44, 0.08)';
export const TICK_COLOR = 'rgba(44, 44, 44, 0.5)';

// ¿Está activo el tema oscuro? (ThemeService pone data-theme en <html>)
export function isDarkTheme(): boolean {
  return (
    typeof document !== 'undefined' &&
    document.documentElement.getAttribute('data-theme') === 'dark'
  );
}

// Colores de texto, rejilla y pistas según el tema. Se pasan a Chart.js como funciones
// para que se lean al dibujar y no al crear la gráfica.
export const inkColor = () => (isDarkTheme() ? '#f5f2eb' : INK);
export const gridColor = () =>
  isDarkTheme() ? 'rgba(245, 242, 235, 0.1)' : GRID_COLOR;
export const tickColor = () =>
  isDarkTheme() ? 'rgba(245, 242, 235, 0.6)' : TICK_COLOR;
export const trackColor = () =>
  isDarkTheme() ? 'rgba(245, 242, 235, 0.08)' : 'rgba(44, 44, 44, 0.07)';
// Valor escrito al final de las barras (el terracota se aclara sobre fondo oscuro)
export const accentTextColor = () => (isDarkTheme() ? '#e58e57' : '#b2571f');

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
    // En tema oscuro el tooltip va un poco más claro que el fondo para que se distinga
    backgroundColor: () => (isDarkTheme() ? '#3a352f' : INK),
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
