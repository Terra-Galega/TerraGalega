import { Component, HostListener, inject } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import {
  EMPTY,
  Subject,
  catchError,
  filter,
  fromEvent,
  merge,
  switchMap,
  timer,
} from 'rxjs';
import { DashboardService } from '../../../../services/dashboard.service';
import { OrderService } from '../../../../services/order.service';
import { ProductService } from '../../../../services/product.service';
import { ToastService } from '../../../../services/toast.service';
import { getErrorMessage } from '../../../../utils/http-error';
import {
  CHART_COLORS,
  DashOrder,
  DashboardModel,
  DelayedOrder,
  DishRow,
  Granularity,
  Kpis,
  PeriodTotals,
  RangeKey,
  RangePair,
  computeKpis,
  compareSeries,
  categoryStats,
  delayedOrders,
  dishRows,
  fmtCompact,
  fmtMoney,
  latestOrders,
  pctChange,
  rangePair,
  salesSeries,
  topProducts,
  totalsFor,
} from '../../../../utils/dashboard-metrics';
import {
  ExportFormat,
  buildReport,
  exportReport,
} from '../../../../utils/dashboard-export';
import {
  ChartSeries,
  SeriesChartComponent,
} from '../../../../components/charts/series-chart/series-chart.component';
import {
  HbarChartComponent,
  RankItem,
} from '../../../../components/charts/hbar-chart/hbar-chart.component';
import {
  DonutChartComponent,
  DonutItem,
} from '../../../../components/charts/donut-chart/donut-chart.component';
import {
  SegmentOption,
  SegmentedControlComponent,
} from '../../../../components/segmented-control/segmented-control.component';
import { DeltaBadgeComponent } from '../../../../components/delta-badge/delta-badge.component';
import { DashboardKpisComponent } from './components/dashboard-kpis/dashboard-kpis.component';
import { DashboardOrdersComponent } from './components/dashboard-orders/dashboard-orders.component';
import { DashboardDishesComponent } from './components/dashboard-dishes/dashboard-dishes.component';

// Cada cuánto se vuelven a pedir los datos al servidor (en milisegundos).
// Es el mismo valor y el mismo patrón que usa el menú del cliente.
const REFRESH_MS = 10_000;

// Dashboard del administrador: indicadores del día, gráficas, últimos pedidos,
// pedidos con mucha espera, tabla de platos con su ganancia y descarga de reportes.
@Component({
  selector: 'app-admin-dashboard',
  imports: [
    SeriesChartComponent,
    HbarChartComponent,
    DonutChartComponent,
    SegmentedControlComponent,
    DeltaBadgeComponent,
    DashboardKpisComponent,
    DashboardOrdersComponent,
    DashboardDishesComponent,
  ],
  templateUrl: './admin-dashboard.component.html',
  styleUrl: './admin-dashboard.component.scss',
})
export class AdminDashboardComponent {
  private dashboardService = inject(DashboardService);
  private orderService = inject(OrderService);
  private productService = inject(ProductService);
  private toast = inject(ToastService);

  // Permite forzar una recarga inmediata (botón de actualizar)
  private reload$ = new Subject<void>();

  // =========================
  // Estado
  // =========================

  model: DashboardModel | null = null;
  errorMessage = '';
  now = new Date();
  lastUpdate: Date | null = null;
  // Segundos desde la última actualización y avance hacia la siguiente (0-100)
  secondsAgo = 0;
  progress = 0;
  loadingNow = false;

  // Selecciones del usuario
  rangeKey: RangeKey = 'today';
  granularity: Granularity = 'day';
  salesType: 'bar' | 'line' = 'bar';
  compareType: 'bar' | 'line' = 'line';
  productMetric: 'units' | 'revenue' = 'units';
  categoryMetric: 'revenue' | 'units' = 'revenue';
  delayThreshold = 30;

  // Exportación
  exportOpen = false;
  exporting: ExportFormat | null = null;

  // =========================
  // Datos ya calculados (se recalculan solo cuando llegan datos o cambia una selección)
  // =========================

  pair: RangePair = rangePair('today', new Date());
  kpis!: Kpis;
  currentTotals!: PeriodTotals;
  previousTotals!: PeriodTotals;
  revenueDelta: number | null = null;

  salesLabels: string[] = [];
  salesTitles: string[] = [];
  salesData: ChartSeries[] = [];

  compareLabels: string[] = [];
  compareData: ChartSeries[] = [];

  topItems: RankItem[] = [];
  categoryItems: DonutItem[] = [];

  latest: DashOrder[] = [];
  delayed: DelayedOrder[] = [];
  dishes: DishRow[] = [];

  // =========================
  // Opciones de los selectores
  // =========================

  readonly rangeOptions: SegmentOption[] = [
    { value: 'today', label: 'Hoy' },
    { value: '7d', label: '7 días' },
    { value: '30d', label: '30 días' },
  ];
  readonly granularityOptions: SegmentOption[] = [
    { value: 'day', label: 'Día' },
    { value: 'week', label: 'Semana' },
    { value: 'month', label: 'Mes' },
  ];
  readonly typeOptions: SegmentOption[] = [
    { value: 'bar', label: 'Barras' },
    { value: 'line', label: 'Línea' },
  ];
  readonly productOptions: SegmentOption[] = [
    { value: 'units', label: 'Unidades' },
    { value: 'revenue', label: 'Ingresos' },
  ];
  readonly categoryOptions: SegmentOption[] = [
    { value: 'revenue', label: 'Ingresos' },
    { value: 'units', label: 'Unidades' },
  ];

  readonly refreshSeconds = REFRESH_MS / 1000;
  readonly money = fmtMoney;
  readonly compact = fmtCompact;
  readonly unitsText = (n: number) => `${Math.round(n)} uds`;
  readonly plainNumber = (n: number) => String(Math.round(n));

  constructor() {
    // Carga los datos al abrir, los actualiza cada REFRESH_MS y también al volver
    // a esta pestaña del navegador. Además se recarga cuando otro componente avisa
    // de cambios en pedidos o productos (por ejemplo, un cambio de precio).
    merge(
      timer(0, REFRESH_MS),
      fromEvent(document, 'visibilitychange'),
      this.reload$,
      this.orderService.refresh$,
      this.productService.refresh$,
    )
      .pipe(
        // Solo se piden datos si la pestaña está visible (no gasta peticiones en segundo plano)
        filter(() => document.visibilityState === 'visible'),
        switchMap(() => {
          this.loadingNow = true;
          // switchMap cancela la petición anterior si llega una nueva antes de terminar
          return this.dashboardService.load().pipe(
            catchError((error) => {
              console.error('Error cargando el dashboard:', error);
              this.errorMessage = getErrorMessage(
                error,
                'No se pudieron cargar los datos del dashboard.',
              );
              this.loadingNow = false;
              // Si falla, se queda lo que ya se veía
              return EMPTY;
            }),
          );
        }),
        // Deja de actualizar cuando se sale del dashboard
        takeUntilDestroyed(),
      )
      .subscribe((model) => {
        this.model = model;
        this.errorMessage = '';
        this.loadingNow = false;
        this.now = new Date();
        this.lastUpdate = this.now;
        this.recompute();
      });

    // Reloj de "actualizado hace X s" y barra de avance hacia la próxima actualización
    timer(0, 1000)
      .pipe(takeUntilDestroyed())
      .subscribe(() => {
        const elapsed = this.lastUpdate
          ? Math.floor((Date.now() - this.lastUpdate.getTime()) / 1000)
          : 0;
        this.secondsAgo = elapsed;
        this.progress = Math.min(
          100,
          ((elapsed + 1) / this.refreshSeconds) * 100,
        );
      });
  }

  // =========================
  // Acciones del usuario
  // =========================

  setRange(value: string) {
    this.rangeKey = value as RangeKey;
    this.recompute();
  }

  setGranularity(value: string) {
    this.granularity = value as Granularity;
    this.recompute();
  }

  setSalesType(value: string) {
    this.salesType = value as 'bar' | 'line';
  }

  setCompareType(value: string) {
    this.compareType = value as 'bar' | 'line';
  }

  setProductMetric(value: string) {
    this.productMetric = value as 'units' | 'revenue';
    this.recompute();
  }

  setCategoryMetric(value: string) {
    this.categoryMetric = value as 'revenue' | 'units';
    this.recompute();
  }

  setThreshold(minutes: number) {
    this.delayThreshold = minutes;
    this.recompute();
  }

  refreshNow() {
    this.reload$.next();
  }

  // Cierra el menú de descarga al hacer clic fuera de él
  @HostListener('document:click')
  closeExportMenu() {
    this.exportOpen = false;
  }

  toggleExportMenu(event: Event) {
    event.stopPropagation();
    this.exportOpen = !this.exportOpen;
  }

  // Genera y descarga el reporte del rango elegido en el formato pedido
  async download(format: ExportFormat) {
    if (!this.model || this.exporting) return;

    this.exporting = format;
    try {
      await exportReport(format, buildReport(this.model, this.pair, this.now));
      this.toast.show('Reporte listo', 'El archivo se descargó correctamente.');
    } catch (error) {
      console.error('Error exportando el reporte:', error);
      this.toast.show(
        'No se pudo exportar',
        'Inténtalo de nuevo en unos segundos.',
      );
    } finally {
      this.exporting = null;
      this.exportOpen = false;
    }
  }

  // =========================
  // Cálculo de lo que se dibuja
  // =========================

  private recompute() {
    const model = this.model;
    if (!model) return;

    const now = this.now;
    this.pair = rangePair(this.rangeKey, now);
    const pair = this.pair;

    // Indicadores del día (no dependen del rango elegido)
    this.kpis = computeKpis(model, now);

    // Totales del rango frente al periodo anterior
    this.currentTotals = totalsFor(model, pair.current);
    this.previousTotals = totalsFor(model, pair.previous);
    this.revenueDelta = pctChange(
      this.currentTotals.revenue,
      this.previousTotals.revenue,
    );

    // Ventas y ganancia por día, semana o mes
    const sales = salesSeries(model, this.granularity, now);
    this.salesLabels = sales.map((p) => p.label);
    this.salesTitles = sales.map((p) => p.title);
    this.salesData = [
      {
        name: 'Ventas',
        values: sales.map((p) => p.revenue),
        color: CHART_COLORS[0],
      },
      {
        name: 'Ganancia',
        values: sales.map((p) => p.profit),
        color: CHART_COLORS[1],
      },
    ];

    // Ingresos del periodo actual frente al anterior
    const compare = compareSeries(model, pair);
    this.compareLabels = compare.labels;
    this.compareData = [
      { name: pair.label, values: compare.current, color: CHART_COLORS[0] },
      {
        name: pair.previousLabel,
        values: compare.previous,
        color: CHART_COLORS[5],
        labels: compare.previousLabels,
        dashed: true,
      },
    ];

    // Productos más vendidos y reparto por categoría
    this.topItems = topProducts(model, pair.current, this.productMetric)
      .slice(0, 6)
      .map((p) => ({
        label: p.name,
        value: p[this.productMetric],
        sub: p.category,
      }));

    this.categoryItems = categoryStats(model, pair.current).map((c, i) => ({
      label: c.name,
      value: c[this.categoryMetric],
      color: CHART_COLORS[i % CHART_COLORS.length],
    }));

    // Pedidos y platos
    this.latest = latestOrders(model, 8);
    this.delayed = delayedOrders(model, now, this.delayThreshold);
    this.dishes = dishRows(model, pair.current);
  }
}