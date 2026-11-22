import { Order, OrderStatus, OrderSummary } from '../models/order';
import { Product } from '../models/product';
import { AdminTableOrder, TablePaymentMethod } from '../models/table-order';
import { isDarkTheme } from './chart-theme';

// =====================================================================
// Cálculos del dashboard del administrador.
// Aquí vive TODA la lógica (sumas, rangos, comparaciones, formato de cifras);
// los componentes solo dibujan lo que esta capa les entrega ya calculado.
// Son funciones puras: reciben el modelo y una fecha, y devuelven datos.
// =====================================================================

// Constantes
// El precio final de cada plato ya incluye un 20 % de ganancia sobre el costo:
// precio = costo X (1 + 0.20)   ->   costo = precio ÷ 1.2
export const PROFIT_MARKUP = 0.2;

// Paleta de las gráficas (marca Terra Galega). El 0 y el 1 son los principales;
// el 5 es un tono apagado para el "periodo anterior".
export const CHART_COLORS = [
  '#b2571f', // terracota
  '#556141', // bosque
  '#d99a21', // dorado
  '#2c6e8f', // azul
  '#8a2222', // granate
  '#9aa089', // salvia apagado
  '#7a5c8a', // violeta
  '#c9885c', // arcilla
];

// Misma paleta, más clara, para verse sobre el fondo del tema oscuro.
// SOLO la usa el dashboard en pantalla: el PDF/Excel exportado sigue con CHART_COLORS.
export const CHART_COLORS_DARK = [
  '#ee9c68', // terracota
  '#adc48f', // bosque
  '#f2c260', // dorado
  '#74b6da', // azul
  '#ec9393', // granate
  '#c8cdb8', // salvia apagado
  '#bf9fd0', // violeta
  '#eab38d', // arcilla
];

// Paleta de las gráficas en pantalla según el tema activo
export function chartColors(): string[] {
  return isDarkTheme() ? CHART_COLORS_DARK : CHART_COLORS;
}

// Colores del estado del pedido (fondo y texto de la etiqueta)
export const STATUS_STYLE: Record<OrderStatus, { bg: string; color: string }> =
  {
    PENDING: { bg: 'rgba(217, 154, 33, 0.2)', color: '#9a6a05' },
    CONFIRMED: { bg: 'rgba(44, 110, 143, 0.14)', color: '#2c6e8f' },
    PREPARING: { bg: 'rgba(178, 87, 31, 0.14)', color: '#b2571f' },
    ON_THE_WAY: { bg: 'rgba(122, 92, 138, 0.16)', color: '#7a5c8a' },
    DELIVERED: { bg: 'rgba(85, 97, 65, 0.16)', color: '#556141' },
    CANCELLED: { bg: 'rgba(138, 34, 34, 0.12)', color: '#8a2222' },
  };

// Las mismas etiquetas en tema oscuro: fondo algo más intenso y texto claro
export const STATUS_STYLE_DARK: Record<
  OrderStatus,
  { bg: string; color: string }
> = {
  PENDING: { bg: 'rgba(242, 194, 96, 0.2)', color: '#f5cd78' },
  CONFIRMED: { bg: 'rgba(116, 182, 218, 0.2)', color: '#94c9e6' },
  PREPARING: { bg: 'rgba(238, 156, 104, 0.22)', color: '#f3ad80' },
  ON_THE_WAY: { bg: 'rgba(191, 159, 208, 0.22)', color: '#d2b9de' },
  DELIVERED: { bg: 'rgba(173, 196, 143, 0.22)', color: '#c2d6a8' },
  CANCELLED: { bg: 'rgba(236, 147, 147, 0.2)', color: '#f2a8a8' },
};

// Estilo de la etiqueta de estado según el tema activo
export function statusStyle(status: OrderStatus): { bg: string; color: string } {
  return (isDarkTheme() ? STATUS_STYLE_DARK : STATUS_STYLE)[status];
}

// Pedidos que todavía no terminaron (ni entregados ni cancelados)
const ACTIVE_STATUSES: OrderStatus[] = [
  'PENDING',
  'CONFIRMED',
  'PREPARING',
  'ON_THE_WAY',
];

const MS_MIN = 60_000;
const MS_HOUR = 3_600_000;
// Tipos
export type RangeKey = 'today' | '7d' | '30d';
// Además de los tres rangos rápidos, el reporte puede ser de un día, una semana o un rango libre
export type PairKey = RangeKey | 'day' | 'week' | 'custom';
export type Granularity = 'day' | 'week' | 'month';

// Intervalo de fechas: incluye `from` y NO incluye `to`
export interface DateRange {
  from: Date;
  to: Date;
}

// Rango elegido junto con el periodo anterior de la misma duración
export interface RangePair {
  key: PairKey;
  label: string;
  previousLabel: string;
  current: DateRange;
  previous: DateRange;
}

// Pedido ya preparado para el dashboard
export interface DashOrder {
  id: number;
  date: Date;
  clientName: string;
  status: OrderStatus;
  // Texto resumen: "2X Ajiaco, 1X Empanadas"
  items: string;
  units: number;
  total: number;
  // Ganancia incluida en el total del pedido
  profit: number;
  // false si está cancelado: no cuenta como venta
  counted: boolean;
  // De dónde viene: domicilio (web) o punto físico (mesero en la app)
  channel: OrderChannel;
  // Solo en punto físico
  tableId?: number;
  waiterName?: string;
  paymentMethod?: TablePaymentMethod | null;
  awaitingPayment?: boolean;
}

export type OrderChannel = 'delivery' | 'table';

export interface DelayedOrder extends DashOrder {
  // Minutos que lleva sin terminar
  waitMin: number;
}

// Una línea de pedido (un plato con sus adicionales)
export interface DashLine {
  orderId: number;
  date: Date;
  counted: boolean;
  productId: number;
  name: string;
  units: number;
  // Ventas de la línea: (precio + adicionales) X cantidad
  revenue: number;
  profit: number;
}

// Producto de la carta (con su precio final actual)
export interface DashProduct {
  id: number;
  name: string;
  category: string;
  price: number;
  active: boolean;
}

export interface DashboardModel {
  orders: DashOrder[];
  lines: DashLine[];
  products: DashProduct[];
}

export interface PeriodTotals {
  revenue: number;
  orders: number;
  units: number;
  profit: number;
}

export interface Kpis {
  salesToday: number;
  ordersToday: number;
  unitsToday: number;
  profitToday: number;
  // Variación frente a ayer HASTA esta misma hora (null = ayer no tiene datos)
  salesDelta: number | null;
  unitsDelta: number | null;
  // Pedidos esperando confirmación y minutos del más antiguo (null = ninguno)
  pending: number;
  pendingOldestMin: number | null;
  // Pedidos que todavía no terminan (pendientes, confirmados, en cocina o en camino)
  activeOrders: number;
}

export interface SalesPoint {
  label: string;
  title: string;
  revenue: number;
  profit: number;
}

export interface CompareSeries {
  labels: string[];
  current: number[];
  previous: number[];
  // Etiqueta larga del periodo anterior para el tooltip
  previousLabels: string[];
}

export interface TopProduct {
  id: number;
  name: string;
  category: string;
  units: number;
  revenue: number;
}

export interface CategoryStat {
  name: string;
  units: number;
  revenue: number;
  profit: number;
}

export interface DishRow {
  id: number;
  name: string;
  category: string;
  active: boolean;
  // Precio final de la carta, costo y ganancia por unidad
  price: number;
  cost: number;
  unitProfit: number;
  units: number;
  sales: number;
  profit: number;
}
// Formato de cifras y fechas
const MONTHS = [
  'ene', 'feb', 'mar', 'abr', 'may', 'jun',
  'jul', 'ago', 'sep', 'oct', 'nov', 'dic',
]; // prettier-ignore

const MONTHS_LONG = [
  'enero', 'febrero', 'marzo', 'abril', 'mayo', 'junio',
  'julio', 'agosto', 'septiembre', 'octubre', 'noviembre', 'diciembre',
]; // prettier-ignore

const WEEKDAYS_LONG = [
  'domingo', 'lunes', 'martes', 'miércoles', 'jueves', 'viernes', 'sábado',
]; // prettier-ignore

const pad = (n: number) => String(n).padStart(2, '0');

// $1,234
export function fmtMoney(n: number): string {
  const v = Math.round(Number.isFinite(n) ? n : 0);
  return `${v < 0 ? '-' : ''}$${Math.abs(v).toLocaleString('en-US')}`;
}

// $950 - $12K - $2.5K - $1.2M (para ejes y para el centro de la dona)
export function fmtCompact(n: number): string {
  const v = Number.isFinite(n) ? n : 0;
  const abs = Math.abs(v);
  const sign = v < 0 ? '-' : '';
  const trim = (x: number) => String(Math.round(x * 10) / 10);

  if (abs >= 1_000_000) return `${sign}$${trim(abs / 1_000_000)}M`;
  if (abs >= 10_000) return `${sign}$${Math.round(abs / 1000)}K`;
  if (abs >= 1_000) return `${sign}$${trim(abs / 1000)}K`;
  return `${sign}$${Math.round(abs)}`;
}

// Variación sin signo (la flecha del DeltaBadge ya indica si sube o baja): 12.4 % - 3 %
export function fmtPct(value: number): string {
  const v = Math.abs(Math.round(value * 10) / 10);
  return `${v} %`;
}

// 3 may
export function shortDate(d: Date): string {
  return `${d.getDate()} ${MONTHS[d.getMonth()]}`;
}

// 3 may 2026
export function fmtDate(d: Date): string {
  return `${shortDate(d)} ${d.getFullYear()}`;
}

// 3 may 2026, 14:30
export function dateTimeLabel(d: Date): string {
  return `${shortDate(d)} ${d.getFullYear()}, ${pad(d.getHours())}:${pad(d.getMinutes())}`;
}

// Minutos transcurridos desde `date` hasta `now` (nunca negativo)
export function minutesSince(date: Date, now: Date): number {
  return Math.max(0, Math.floor((now.getTime() - date.getTime()) / MS_MIN));
}

// 12 min - 1 h 5 min - 2 d 3 h
export function formatWait(minutes: number): string {
  const m = Math.max(0, Math.round(minutes));
  if (m < 60) return `${m} min`;
  const h = Math.floor(m / 60);
  if (h < 24) return m % 60 ? `${h} h ${m % 60} min` : `${h} h`;
  const d = Math.floor(h / 24);
  return h % 24 ? `${d} d ${h % 24} h` : `${d} d`;
}

// Variación porcentual (12.4 = +12.4 %).
// null cuando el periodo anterior no tiene datos con qué comparar.
export function pctChange(current: number, previous: number): number | null {
  if (previous === 0) return current === 0 ? 0 : null;
  return ((current - previous) / previous) * 100;
}
// Fechas
// Mismo momento, `days` días después (negativo = antes). Respeta cambios de hora.
export function addDays(date: Date, days: number): Date {
  const d = new Date(date);
  d.setDate(d.getDate() + days);
  return d;
}

export function startOfDay(date: Date): Date {
  const d = new Date(date);
  d.setHours(0, 0, 0, 0);
  return d;
}

// Lunes de la semana de `date` (a las 00:00)
export function startOfWeek(date: Date): Date {
  const d = startOfDay(date);
  d.setDate(d.getDate() - ((d.getDay() + 6) % 7));
  return d;
}

function startOfMonth(date: Date): Date {
  const d = startOfDay(date);
  d.setDate(1);
  return d;
}

function inRange(date: Date, range: DateRange): boolean {
  const t = date.getTime();
  return t >= range.from.getTime() && t < range.to.getTime();
}

// El rango elegido y el periodo anterior de igual duración:
//   Hoy     -> hoy frente a ayer
//   7 días  -> últimos 7 días frente a los 7 anteriores
//   30 días -> ídem con 30
export function rangePair(key: RangeKey, now: Date): RangePair {
  const today = startOfDay(now);
  const tomorrow = addDays(today, 1);

  if (key === 'today') {
    return {
      key,
      label: 'Hoy',
      previousLabel: 'Ayer',
      current: { from: today, to: tomorrow },
      previous: { from: addDays(today, -1), to: today },
    };
  }

  const days = key === '7d' ? 7 : 30;
  const from = addDays(tomorrow, -days);
  return {
    key,
    label: `${days} días`,
    previousLabel: `${days} días anteriores`,
    current: { from, to: tomorrow },
    previous: { from: addDays(from, -days), to: from },
  };
}

// Periodo elegido a mano para el reporte: de `first` a `last` (ambos días incluidos).
// El periodo anterior es el tramo inmediato de igual duración, para poder comparar.
export function customPair(
  key: 'day' | 'week' | 'custom',
  first: Date,
  last: Date,
): RangePair {
  const from = startOfDay(first);
  const to = addDays(startOfDay(last), 1);
  const days = Math.max(
    1,
    Math.round((to.getTime() - from.getTime()) / (24 * MS_HOUR)),
  );
  const prevFrom = addDays(from, -days);

  let label: string;
  let previousLabel: string;
  if (key === 'day') {
    label = fmtDate(from);
    previousLabel = fmtDate(prevFrom);
  } else if (key === 'week') {
    label = `Semana del ${shortDate(from)} al ${fmtDate(addDays(to, -1))}`;
    previousLabel = 'Semana anterior';
  } else {
    label = `${fmtDate(from)} - ${fmtDate(addDays(to, -1))}`;
    previousLabel = `${days} ${days === 1 ? 'día anterior' : 'días anteriores'}`;
  }

  return {
    key,
    label,
    previousLabel,
    current: { from, to },
    previous: { from: prevFrom, to: from },
  };
}
// Construcción del modelo
// Parte del precio final que es ganancia: precio X 0.2 ÷ 1.2
const PROFIT_SHARE = PROFIT_MARKUP / (1 + PROFIT_MARKUP);

function num(value: unknown): number {
  const n = Number(value);
  return Number.isFinite(n) ? n : 0;
}

// Une los tres orígenes de datos en un único modelo ya calculado:
//  - orders: platos, cantidades, adicionales y total de cada pedido
//  - summaries: traen el nombre del cliente
//  - products: traen la categoría y el precio final actual
export function buildModel(
  orders: Order[],
  summaries: OrderSummary[],
  products: Product[],
  tableOrders: AdminTableOrder[] = [],
): DashboardModel {
  const clientNames = new Map<number, string>();
  summaries.forEach((s) => {
    if (s.clientName) clientNames.set(s.id, s.clientName);
  });

  const categoryOf = new Map<number, string>();
  const dashProducts: DashProduct[] = products.map((p) => {
    const category = p.category?.name ?? 'Sin categoría';
    categoryOf.set(p.id, category);
    return {
      id: p.id,
      name: p.name,
      category,
      price: num(p.price),
      active: !!p.active,
    };
  });

  const lines: DashLine[] = [];
  const dashOrders: DashOrder[] = [];

  for (const o of orders) {
    const date = new Date(o.createdAt);
    // Un pedido sin fecha válida no se puede ubicar en ningún periodo
    if (Number.isNaN(date.getTime())) continue;

    const counted = o.status !== 'CANCELLED';
    const details = o.details ?? [];

    let units = 0;
    let linesTotal = 0;
    for (const d of details) {
      const qty = num(d.quantity);
      const addOns = (d.addOns ?? []).reduce((s, a) => s + num(a.price), 0);
      const revenue = (num(d.unitPrice) + addOns) * qty;
      units += qty;
      linesTotal += revenue;
      lines.push({
        orderId: o.id,
        date,
        counted,
        productId: d.product.id,
        name: d.product.name,
        units: qty,
        revenue,
        profit: revenue * PROFIT_SHARE,
      });
    }

    const total =
      o.total != null && Number.isFinite(Number(o.total))
        ? Number(o.total)
        : linesTotal;

    const clientName =
      clientNames.get(o.id) ??
      (o.client ? `${o.client.name} ${o.client.lastName}`.trim() : '');

    dashOrders.push({
      id: o.id,
      date,
      clientName: clientName || 'Cliente',
      status: o.status,
      items: details.map((d) => `${d.quantity}× ${d.product.name}`).join(', '),
      units,
      total,
      profit: total * PROFIT_SHARE,
      counted,
      channel: 'delivery',
    });
  }

  // Órdenes en punto físico: cuentan en las ventas, productos y categorías igual
  // que los domicilios. Su estado se traduce al más parecido de un domicilio
  // (en cocina = en preparación, entregado = entregado) para las etiquetas.
  for (const t of tableOrders) {
    const date = new Date(t.startTime);
    if (Number.isNaN(date.getTime())) continue;

    const counted = t.status !== 'CANCELLED';
    for (const i of t.items ?? []) {
      const revenue = num(i.subtotal);
      lines.push({
        orderId: t.id,
        date,
        counted,
        productId: i.productId,
        name: i.productName,
        units: num(i.quantity),
        revenue,
        profit: revenue * PROFIT_SHARE,
      });
    }

    const total = num(t.total);
    dashOrders.push({
      id: t.id,
      date,
      clientName: `Mesa ${t.tableId} - ${t.waiterName}`,
      status:
        t.status === 'CANCELLED'
          ? 'CANCELLED'
          : t.status === 'DONE'
            ? 'DELIVERED'
            : 'PREPARING',
      items: (t.items ?? [])
        .map((i) => `${i.quantity}× ${i.productName}`)
        .join(', '),
      units: num(t.units),
      total,
      profit: total * PROFIT_SHARE,
      counted,
      channel: 'table',
      tableId: t.tableId,
      waiterName: t.waiterName,
      paymentMethod: t.paymentMethod,
      awaitingPayment: t.awaitingPayment,
    });
  }

  return { orders: dashOrders, lines, products: dashProducts };
}
// Totales e indicadores
// Ventas, pedidos, unidades y ganancia de un intervalo (los cancelados no cuentan)
export function totalsFor(
  model: DashboardModel,
  range: DateRange,
): PeriodTotals {
  const totals: PeriodTotals = { revenue: 0, orders: 0, units: 0, profit: 0 };
  for (const o of model.orders) {
    if (!o.counted || !inRange(o.date, range)) continue;
    totals.revenue += o.total;
    totals.orders += 1;
    totals.units += o.units;
    totals.profit += o.profit;
  }
  return totals;
}

// Indicadores del día. La comparación es contra ayer HASTA la misma hora,
// para que a las 10 a. m. no se compare medio día contra un día completo.
export function computeKpis(model: DashboardModel, now: Date): Kpis {
  const today = startOfDay(now);
  const yesterday = addDays(today, -1);
  const sameTimeYesterday = addDays(now, -1);

  const todayTotals = totalsFor(model, { from: today, to: addDays(today, 1) });
  const yesterdayTotals = totalsFor(model, {
    from: yesterday,
    to: new Date(sameTimeYesterday.getTime() + 1),
  });

  let pending = 0;
  let active = 0;
  let oldest: number | null = null;
  for (const o of model.orders) {
    if (!ACTIVE_STATUSES.includes(o.status)) continue;
    active++;
    if (o.status === 'PENDING') {
      pending++;
      const wait = minutesSince(o.date, now);
      if (oldest === null || wait > oldest) oldest = wait;
    }
  }

  return {
    salesToday: todayTotals.revenue,
    ordersToday: todayTotals.orders,
    unitsToday: todayTotals.units,
    profitToday: todayTotals.profit,
    salesDelta: pctChange(todayTotals.revenue, yesterdayTotals.revenue),
    unitsDelta: pctChange(todayTotals.units, yesterdayTotals.units),
    pending,
    pendingOldestMin: oldest,
    activeOrders: active,
  };
}
// Series para las gráficas
// Ventas y ganancia por día (14), semana (8) o mes (6), terminando en el actual.
export function salesSeries(
  model: DashboardModel,
  granularity: Granularity,
  now: Date,
): SalesPoint[] {
  const slots: { range: DateRange; label: string; title: string }[] = [];

  if (granularity === 'day') {
    const today = startOfDay(now);
    for (let i = 13; i >= 0; i--) {
      const from = addDays(today, -i);
      slots.push({
        range: { from, to: addDays(from, 1) },
        label: shortDate(from),
        title: `${cap(WEEKDAYS_LONG[from.getDay()])} ${from.getDate()} de ${MONTHS_LONG[from.getMonth()]}`,
      });
    }
  } else if (granularity === 'week') {
    const monday = startOfWeek(now);
    for (let i = 7; i >= 0; i--) {
      const from = addDays(monday, -7 * i);
      const to = addDays(from, 7);
      slots.push({
        range: { from, to },
        label: shortDate(from),
        title: `Semana del ${shortDate(from)} al ${shortDate(addDays(to, -1))}`,
      });
    }
  } else {
    const first = startOfMonth(now);
    for (let i = 5; i >= 0; i--) {
      const from = new Date(first.getFullYear(), first.getMonth() - i, 1);
      const to = new Date(from.getFullYear(), from.getMonth() + 1, 1);
      slots.push({
        range: { from, to },
        label: MONTHS[from.getMonth()],
        title: `${cap(MONTHS_LONG[from.getMonth()])} ${from.getFullYear()}`,
      });
    }
  }

  return slots.map(({ range, label, title }) => {
    const t = totalsFor(model, range);
    return { label, title, revenue: t.revenue, profit: t.profit };
  });
}

// Ingresos del periodo actual frente al anterior, posición por posición.
//  - Hoy: hora por hora, solo hasta la hora actual (igual que "vs ayer a esta hora")
//  - 7 / 30 días: día por día
export function compareSeries(
  model: DashboardModel,
  pair: RangePair,
): CompareSeries {
  const result: CompareSeries = {
    labels: [],
    current: [],
    previous: [],
    previousLabels: [],
  };
  const revenue = (from: Date, to: Date) =>
    totalsFor(model, { from, to }).revenue;

  const spanDays = Math.round(
    (pair.current.to.getTime() - pair.current.from.getTime()) / (24 * MS_HOUR),
  );

  // Un solo día (hoy, ayer o cualquier fecha): hora por hora. Si es hoy, solo hasta la hora actual.
  if (spanDays === 1) {
    const now = new Date();
    const isToday = startOfDay(now).getTime() === pair.current.from.getTime();
    const lastHour = isToday ? now.getHours() : 23;
    for (let h = 0; h <= lastHour; h++) {
      const cur = new Date(pair.current.from);
      cur.setHours(h);
      const prev = new Date(pair.previous.from);
      prev.setHours(h);
      result.labels.push(`${pad(h)}:00`);
      result.previousLabels.push(`${pair.previousLabel} ${pad(h)}:00`);
      result.current.push(revenue(cur, new Date(cur.getTime() + MS_HOUR)));
      result.previous.push(revenue(prev, new Date(prev.getTime() + MS_HOUR)));
    }
    return result;
  }

  for (let i = 0; i < spanDays; i++) {
    const cur = addDays(pair.current.from, i);
    const prev = addDays(pair.previous.from, i);
    result.labels.push(shortDate(cur));
    result.previousLabels.push(shortDate(prev));
    result.current.push(revenue(cur, addDays(cur, 1)));
    result.previous.push(revenue(prev, addDays(prev, 1)));
  }
  return result;
}

// Ventas y ganancia de cualquier intervalo, en barras de tamaño adecuado:
// un día -> por hora - hasta 62 días -> por día - más -> por semana.
export function rangeSeries(
  model: DashboardModel,
  range: DateRange,
): { labels: string[]; revenue: number[]; profit: number[] } {
  const out = {
    labels: [] as string[],
    revenue: [] as number[],
    profit: [] as number[],
  };
  const push = (label: string, from: Date, to: Date) => {
    const t = totalsFor(model, { from, to });
    out.labels.push(label);
    out.revenue.push(t.revenue);
    out.profit.push(t.profit);
  };

  const days = Math.round(
    (range.to.getTime() - range.from.getTime()) / (24 * MS_HOUR),
  );
  if (days <= 1) {
    for (let h = 0; h < 24; h++) {
      const from = new Date(range.from);
      from.setHours(h);
      push(`${pad(h)}:00`, from, new Date(from.getTime() + MS_HOUR));
    }
  } else if (days <= 62) {
    for (let i = 0; i < days; i++) {
      const from = addDays(range.from, i);
      push(shortDate(from), from, addDays(from, 1));
    }
  } else {
    for (let from = range.from; from < range.to; from = addDays(from, 7)) {
      const to = addDays(from, 7);
      push(shortDate(from), from, to < range.to ? to : range.to);
    }
  }
  return out;
}
// Productos, categorías y platos
interface DishAgg {
  units: number;
  revenue: number;
  profit: number;
}

// Unidades, ventas y ganancia por producto en un intervalo
function aggregateByProduct(
  model: DashboardModel,
  range: DateRange,
): Map<number, DishAgg & { name: string }> {
  const map = new Map<number, DishAgg & { name: string }>();
  for (const l of model.lines) {
    if (!l.counted || !inRange(l.date, range)) continue;
    const a = map.get(l.productId) ?? {
      name: l.name,
      units: 0,
      revenue: 0,
      profit: 0,
    };
    a.units += l.units;
    a.revenue += l.revenue;
    a.profit += l.profit;
    map.set(l.productId, a);
  }
  return map;
}

const NO_CATEGORY = 'Sin categoría';

function categoryName(model: DashboardModel, productId: number): string {
  return (
    model.products.find((p) => p.id === productId)?.category ?? NO_CATEGORY
  );
}

// Productos con ventas en el intervalo, de mayor a menor según la métrica
export function topProducts(
  model: DashboardModel,
  range: DateRange,
  metric: 'units' | 'revenue',
): TopProduct[] {
  return [...aggregateByProduct(model, range)]
    .map(([id, a]) => ({
      id,
      name: a.name,
      category: categoryName(model, id),
      units: a.units,
      revenue: a.revenue,
    }))
    .filter((p) => p.units > 0)
    .sort((a, b) => b[metric] - a[metric] || a.name.localeCompare(b.name));
}

// Ventas por categoría en el intervalo, de mayor a menor ingreso
export function categoryStats(
  model: DashboardModel,
  range: DateRange,
): CategoryStat[] {
  const map = new Map<string, CategoryStat>();
  for (const [id, a] of aggregateByProduct(model, range)) {
    const name = categoryName(model, id);
    const c = map.get(name) ?? { name, units: 0, revenue: 0, profit: 0 };
    c.units += a.units;
    c.revenue += a.revenue;
    c.profit += a.profit;
    map.set(name, c);
  }
  return [...map.values()].sort(
    (a, b) => b.revenue - a.revenue || a.name.localeCompare(b.name),
  );
}

// Todos los platos de la carta (incluso los que no se vendieron) con su precio final,
// su costo, la ganancia por unidad y lo vendido en el intervalo.
export function dishRows(model: DashboardModel, range: DateRange): DishRow[] {
  const sold = aggregateByProduct(model, range);

  const rows: DishRow[] = model.products.map((p) => {
    const a = sold.get(p.id);
    const cost = p.price / (1 + PROFIT_MARKUP);
    return {
      id: p.id,
      name: p.name,
      category: p.category,
      active: p.active,
      price: p.price,
      cost,
      unitProfit: p.price - cost,
      units: a?.units ?? 0,
      sales: a?.revenue ?? 0,
      profit: a?.profit ?? 0,
    };
  });

  // Productos que aparecen en pedidos pero ya no están en la carta
  const known = new Set(model.products.map((p) => p.id));
  for (const [id, a] of sold) {
    if (known.has(id)) continue;
    rows.push({
      id,
      name: a.name,
      category: NO_CATEGORY,
      active: false,
      price: 0,
      cost: 0,
      unitProfit: 0,
      units: a.units,
      sales: a.revenue,
      profit: a.profit,
    });
  }
  return rows;
}
// Pedidos
// Los `limit` pedidos más recientes (cancelados incluidos: el admin quiere verlos)
export function latestOrders(
  model: DashboardModel,
  limit: number,
): DashOrder[] {
  return [...model.orders]
    .sort((a, b) => b.date.getTime() - a.date.getTime())
    .slice(0, limit);
}

// Pedidos sin terminar que llevan al menos `thresholdMin` minutos, el más viejo primero
export function delayedOrders(
  model: DashboardModel,
  now: Date,
  thresholdMin: number,
): DelayedOrder[] {
  return model.orders
    .filter((o) => ACTIVE_STATUSES.includes(o.status))
    .map((o) => ({ ...o, waitMin: minutesSince(o.date, now) }))
    .filter((o) => o.waitMin >= thresholdMin)
    .sort((a, b) => b.waitMin - a.waitMin);
}

function cap(text: string): string {
  return text.charAt(0).toUpperCase() + text.slice(1);
}

// Domicilio frente a punto físico
export interface ChannelTotals {
  revenue: number;
  orders: number;
  units: number;
  average: number;
}

export interface WaiterStat {
  name: string;
  orders: number;
  revenue: number;
}

export interface ChannelStats {
  delivery: ChannelTotals;
  table: ChannelTotals;
  // Solo punto físico: cobrado en efectivo, con tarjeta y entregado sin cobrar
  cash: number;
  card: number;
  unpaid: number;
  // Meseros con más ventas en el intervalo
  waiters: WaiterStat[];
}

// Ventas de cada canal en el intervalo (los cancelados no cuentan)
export function channelStats(
  model: DashboardModel,
  range: DateRange,
): ChannelStats {
  const empty = (): ChannelTotals => ({
    revenue: 0,
    orders: 0,
    units: 0,
    average: 0,
  });
  const stats: ChannelStats = {
    delivery: empty(),
    table: empty(),
    cash: 0,
    card: 0,
    unpaid: 0,
    waiters: [],
  };
  const waiters = new Map<string, WaiterStat>();

  for (const o of model.orders) {
    if (!o.counted || !inRange(o.date, range)) continue;
    const c = stats[o.channel];
    c.revenue += o.total;
    c.orders += 1;
    c.units += o.units;

    if (o.channel !== 'table') continue;
    if (o.paymentMethod === 'CASH') stats.cash += o.total;
    else if (o.paymentMethod === 'CARD') stats.card += o.total;
    else if (o.awaitingPayment) stats.unpaid += o.total;

    const name = o.waiterName || 'Mesero';
    const w = waiters.get(name) ?? { name, orders: 0, revenue: 0 };
    w.orders += 1;
    w.revenue += o.total;
    waiters.set(name, w);
  }

  for (const c of [stats.delivery, stats.table]) {
    c.average = c.orders ? c.revenue / c.orders : 0;
  }
  stats.waiters = [...waiters.values()].sort(
    (a, b) => b.revenue - a.revenue || a.name.localeCompare(b.name),
  );
  return stats;
}
