import { Component, effect, inject, untracked } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { ThemeService } from '../../../../services/theme.service';
import { PaletteService } from '../../../../services/palette.service';
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
import {
  chartColors,
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
  channelStats,
  ChannelStats,
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
import { buildReport, exportReport } from '../../../../utils/dashboard-export';
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
import { DashboardChannelsComponent } from './components/dashboard-channels/dashboard-channels.component';
import {
  DashboardExportModalComponent,
  ExportRequest,
  EmailReportRequest,
} from './components/dashboard-export-modal/dashboard-export-modal.component';
import { firstValueFrom } from 'rxjs';
import {
  ArcElement,
  BarController,
  BarElement,
  CategoryScale,
  DoughnutController,
  Filler,
  LineController,
  LineElement,
  LinearScale,
  PointElement,
  Tooltip,
} from 'chart.js';
import { provideCharts } from 'ng2-charts';
import { exportReportToBlob } from '../../../../utils/dashboard-export'; // súmalo al import que ya existe de buildReport/exportReport
import { getErrorMessage } from '../../../../utils/http-error';
import { ReportService } from '../../../../services/report.service';

// Cada cuánto se vuelven a pedir los datos al servidor (en milisegundos).
// Es el mismo valor y el mismo patrón que usa el menú del cliente.
const REFRESH_MS = 10_000;

// Equivalencia entre el periodo de cada tarjeta (Día / Semana / Mes) y el rango que se calcula
const PERIOD_TO_RANGE: Record<Granularity, RangeKey> = {
  day: 'today',
  week: '7d',
  month: '30d',
};
const RANGE_TO_PERIOD: Record<RangeKey, Granularity> = {
  today: 'day',
  '7d': 'week',
  '30d': 'month',
};

// Dashboard del administrador: indicadores del día, gráficas, últimos pedidos,
// pedidos con mucha espera, tabla de platos con su ganancia y descarga de reportes.
@Component({
  // Chart.js se registra aquí y no en app.config: así la librería solo se descarga al abrir
  // el dashboard y no pesa en la carga inicial de la web. Se registran solo las piezas que
  // usan las gráficas (barras, líneas, dona, ejes y tooltip).
  providers: [
    provideCharts({
      registerables: [
        BarController,
        BarElement,
        LineController,
        LineElement,
        PointElement,
        DoughnutController,
        ArcElement,
        CategoryScale,
        LinearScale,
        Filler,
        Tooltip,
      ],
    }),
  ],
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
    DashboardChannelsComponent,
    DashboardExportModalComponent,
  ],
  templateUrl: './admin-dashboard.component.html',
  styleUrl: './admin-dashboard.component.scss',
})
export class AdminDashboardComponent {
  private dashboardService = inject(DashboardService);
  private orderService = inject(OrderService);
  private productService = inject(ProductService);
  private toast = inject(ToastService);
  private reports = inject(ReportService);
  private themeService = inject(ThemeService);
  private paletteService = inject(PaletteService);

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

  // Periodo propio (Día / Semana / Mes) de cada tarjeta. Siguen al selector
  // general cuando este cambia, pero se pueden ajustar una por una.
  comparePeriod: Granularity = 'day';
  productPeriod: Granularity = 'day';
  categoryPeriod: Granularity = 'day';

  // Exportación
  exportOpen = false;
  exporting = false;

  // =========================
  // Datos ya calculados (se recalculan solo cuando llegan datos o cambia una selección)
  // =========================

  pair: RangePair = rangePair('today', new Date());
  comparePair: RangePair = this.pair;
  productPair: RangePair = this.pair;
  categoryPair: RangePair = this.pair;
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
  // Domicilio vs. punto físico del periodo elegido
  channels: ChannelStats | null = null;

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
    // Al cambiar entre tema claro y oscuro (el interruptor de la barra superior) se vuelven a
    // calcular los datos de las gráficas para que tomen la paleta del tema nuevo.
    effect(() => {
      this.themeService.theme();
      this.paletteService.palette();
      untracked(() => this.recompute());
    });

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
    // Las tarjetas con selector propio pasan al periodo equivalente
    const period = RANGE_TO_PERIOD[this.rangeKey];
    this.comparePeriod = period;
    this.productPeriod = period;
    this.categoryPeriod = period;
    this.recompute();
  }

  setComparePeriod(value: string) {
    this.comparePeriod = value as Granularity;
    this.recompute();
  }

  setProductPeriod(value: string) {
    this.productPeriod = value as Granularity;
    this.recompute();
  }

  setCategoryPeriod(value: string) {
    this.categoryPeriod = value as Granularity;
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

  openExport() {
    this.exportOpen = true;
  }

  closeExport() {
    if (!this.exporting) this.exportOpen = false;
  }

  // Genera y descarga el reporte del periodo y formato elegidos en la ventana
  async download(request: ExportRequest) {
    if (!this.model || this.exporting) return;

    let previewWindow: Window | null = null;
    this.exporting = true;

    try {
      // Abre la pestaña directamente desde el clic,
      // antes de generar las gráficas o cargar las librerías.
      if (request.format === 'pdf') {
        previewWindow = window.open('about:blank', '_blank');

        if (!previewWindow) {
          throw new Error(
            'El navegador bloqueó la vista previa. ' +
              'Permite las ventanas emergentes para este sitio ' +
              'y vuelve a intentarlo.',
          );
        }

        previewWindow.opener = null;

        previewWindow.document.title = 'Generando reporte — Terra Galega';

        previewWindow.document.body.textContent =
          'Generando vista previa del PDF…';
      }

      await exportReport(
        request.format,
        buildReport(
          this.model,
          request.pair,
          new Date(),
          request.includeCharts,
        ),
        previewWindow,
      );

      this.toast.show(
        'Reporte listo',
        request.format === 'pdf'
          ? 'El PDF se abrió en otra pestaña. Desde allí puedes descargarlo o imprimirlo.'
          : 'El archivo se descargó correctamente.',
      );

      this.exportOpen = false;
    } catch (error) {
      // Si la generación falla, no deja una pestaña vacía abierta.
      if (previewWindow && !previewWindow.closed) {
        previewWindow.close();
      }

      console.error('Error exportando el reporte:', error);

      this.toast.show(
        'No se pudo exportar',
        error instanceof Error
          ? error.message
          : 'Inténtalo de nuevo en unos segundos.',
      );
    } finally {
      this.exporting = false;
    }
  }

  // Genera el informe elegido y lo envía al correo escrito en la ventana
  async sendEmail(request: EmailReportRequest) {
    if (!this.model || this.exporting) return;
    this.exporting = true;

    try {
      const report = buildReport(
        this.model,
        request.pair,
        new Date(),
        request.includeCharts,
      );
      const { blob, filename } = await exportReportToBlob(
        request.format,
        report,
      );

      await firstValueFrom(
        this.reports.emailReport(
          request.to,
          'Informe de ventas - Terra Galega',
          blob,
          filename,
        ),
      );

      this.toast.show('Informe enviado', `Lo enviamos a ${request.to}.`);
      this.exportOpen = false;
    } catch (error) {
      console.error('Error enviando el informe:', error);
      this.toast.show(
        'No se pudo enviar',
        getErrorMessage(error, 'Inténtalo de nuevo en unos segundos.'),
      );
    } finally {
      this.exporting = false;
    }
  }

  // =========================
  // Cálculo de lo que se dibuja
  // =========================

  private recompute() {
    const model = this.model;
    if (!model) return;

    const now = this.now;
    // Colores de las gráficas en pantalla (más claros en tema oscuro)
    const palette = chartColors();
    this.pair = rangePair(this.rangeKey, now);
    const pair = this.pair;
    this.comparePair = rangePair(PERIOD_TO_RANGE[this.comparePeriod], now);
    this.productPair = rangePair(PERIOD_TO_RANGE[this.productPeriod], now);
    this.categoryPair = rangePair(PERIOD_TO_RANGE[this.categoryPeriod], now);
    const cmp = this.comparePair;

    // Indicadores del día (no dependen del rango elegido)
    this.kpis = computeKpis(model, now);

    // Totales del periodo de la comparación frente al anterior
    this.currentTotals = totalsFor(model, cmp.current);
    this.previousTotals = totalsFor(model, cmp.previous);
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
        color: palette[0],
      },
      {
        name: 'Ganancia',
        values: sales.map((p) => p.profit),
        color: palette[1],
      },
    ];

    // Ingresos del periodo actual frente al anterior
    const compare = compareSeries(model, cmp);
    this.compareLabels = compare.labels;
    this.compareData = [
      { name: cmp.label, values: compare.current, color: palette[0] },
      {
        name: cmp.previousLabel,
        values: compare.previous,
        color: palette[5],
        labels: compare.previousLabels,
        dashed: true,
      },
    ];

    // Productos más vendidos y reparto por categoría
    this.topItems = topProducts(
      model,
      this.productPair.current,
      this.productMetric,
    )
      .slice(0, 6)
      .map((p) => ({
        label: p.name,
        value: p[this.productMetric],
        sub: p.category,
      }));

    this.categoryItems = categoryStats(model, this.categoryPair.current).map(
      (c, i) => ({
        label: c.name,
        value: c[this.categoryMetric],
        color: palette[i % palette.length],
      }),
    );

    // Pedidos y platos
    this.latest = latestOrders(model, 8);
    this.delayed = delayedOrders(model, now, this.delayThreshold);
    this.dishes = dishRows(model, pair.current);
    this.channels = channelStats(model, pair.current);
  }
}
