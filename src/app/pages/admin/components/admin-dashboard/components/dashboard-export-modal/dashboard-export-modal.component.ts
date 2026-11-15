import {
  Component,
  EventEmitter,
  HostListener,
  Input,
  OnInit,
  Output,
} from '@angular/core';
import { DialogDirective } from '../../../../../../directives/dialog.directive';
import { ButtonComponent } from '../../../../../../components/button/button.component';
import { CheckOptionComponent } from '../../../../../../components/check-option/check-option.component';
import { IconButtonComponent } from '../../../../../../components/icon-button/icon-button.component';
import {
  SegmentOption,
  SegmentedControlComponent,
} from '../../../../../../components/segmented-control/segmented-control.component';
import {
  DashboardModel,
  PeriodTotals,
  RangePair,
  addDays,
  customPair,
  fmtDate,
  fmtMoney,
  startOfDay,
  startOfWeek,
  totalsFor,
} from '../../../../../../utils/dashboard-metrics';
import { ExportFormat } from '../../../../../../utils/dashboard-export';
import {
  EMAIL_ERROR_MESSAGE,
  isValidEmail,
} from '../../../../../../utils/email';

// Lo que el modal le entrega al dashboard al confirmar
export interface ExportRequest {
  format: ExportFormat;
  pair: RangePair;
  includeCharts: boolean;
}

// Igual que ExportRequest, pero en vez de descargar se envía a este correo
export interface EmailReportRequest extends ExportRequest {
  to: string;
}

type PeriodMode = 'day' | 'week' | 'range';

// Un periodo rápido (botón con un clic)
interface Preset {
  label: string;
  apply: () => void;
}

const MAX_DAYS = 366;

// yyyy-mm-dd, que es lo que usa <input type="date">
function toInput(d: Date): string {
  const p = (n: number) => String(n).padStart(2, '0');
  return `${d.getFullYear()}-${p(d.getMonth() + 1)}-${p(d.getDate())}`;
}

// Convierte el texto del input a una fecha local (null si está vacío o es inválido)
function fromInput(value: string): Date | null {
  const m = /^(\d{4})-(\d{2})-(\d{2})$/.exec(value);
  if (!m) return null;
  const d = new Date(Number(m[1]), Number(m[2]) - 1, Number(m[3]));
  return Number.isNaN(d.getTime()) ? null : d;
}

// Ventana para elegir qué periodo se descarga (un día, una semana o un rango de
// fechas), en qué formato y si el archivo lleva las gráficas.
@Component({
  selector: 'app-dashboard-export-modal',
  imports: [DialogDirective, 
    ButtonComponent,
    CheckOptionComponent,
    IconButtonComponent,
    SegmentedControlComponent,
  ],
  templateUrl: './dashboard-export-modal.component.html',
  styleUrl: './dashboard-export-modal.component.scss',
})
export class DashboardExportModalComponent implements OnInit {
  @Input({ required: true }) model!: DashboardModel;
  @Input() now = new Date();
  // true mientras se genera el archivo (bloquea los botones)
  @Input() exporting = false;

  @Output() closed = new EventEmitter<void>();
  @Output() confirmed = new EventEmitter<ExportRequest>();

  @Output() emailed = new EventEmitter<EmailReportRequest>();

  emailTo = '';
  emailError = '';

  readonly modeOptions: SegmentOption[] = [
    { value: 'day', label: 'Por día' },
    { value: 'week', label: 'Por semana' },
    { value: 'range', label: 'Rango de fechas' },
  ];

  readonly formats: {
    value: ExportFormat;
    badge: string;
    title: string;
    text: string;
    bg: string;
    color: string;
  }[] = [
    {
      value: 'pdf',
      badge: 'PDF',
      title: 'PDF',
      text: 'Reporte con logo y gráficas',
      bg: 'rgba(138, 34, 34, 0.12)',
      color: '#8a2222',
    },
    {
      value: 'excel',
      badge: 'XLS',
      title: 'Excel',
      text: 'Hojas con datos y gráficas',
      bg: 'rgba(85, 97, 65, 0.16)',
      color: '#556141',
    },
    {
      value: 'csv',
      badge: 'CSV',
      title: 'CSV',
      text: 'Solo datos, sin gráficas',
      bg: 'rgba(44, 44, 44, 0.1)',
      color: '#2c2c2c',
    },
  ];

  mode: PeriodMode = 'day';
  format: ExportFormat = 'pdf';
  includeCharts = true;

  // Fechas elegidas (texto yyyy-mm-dd de los inputs)
  dayValue = '';
  weekValue = '';
  fromValue = '';
  toValue = '';
  // Hoy, como tope de los selectores de fecha
  maxDate = '';

  // Resultado de lo elegido
  pair: RangePair | null = null;
  totals: PeriodTotals | null = null;
  error = '';

  readonly money = fmtMoney;

  constructor() {
    const today = startOfDay(new Date());
    this.maxDate = toInput(today);
    this.dayValue = this.weekValue = this.toValue = this.maxDate;
    this.fromValue = toInput(addDays(today, -6));
  }

  // El modelo llega como @Input, así que se calcula la vista previa cuando ya está disponible
  ngOnInit() {
    this.recalc();
  }

  // Atajos de cada modo
  get presets(): Preset[] {
    const today = startOfDay(new Date());
    if (this.mode === 'day') {
      return [
        { label: 'Hoy', apply: () => this.setDayTo(today) },
        { label: 'Ayer', apply: () => this.setDayTo(addDays(today, -1)) },
        {
          label: 'Hace 2 días',
          apply: () => this.setDayTo(addDays(today, -2)),
        },
      ];
    }
    if (this.mode === 'week') {
      return [
        { label: 'Esta semana', apply: () => this.setWeekTo(today) },
        {
          label: 'Semana pasada',
          apply: () => this.setWeekTo(addDays(today, -7)),
        },
        {
          label: 'Hace 2 semanas',
          apply: () => this.setWeekTo(addDays(today, -14)),
        },
      ];
    }
    return [
      {
        label: 'Últimos 7 días',
        apply: () => this.setRangeTo(addDays(today, -6), today),
      },
      {
        label: 'Últimos 30 días',
        apply: () => this.setRangeTo(addDays(today, -29), today),
      },
      {
        label: 'Este mes',
        apply: () =>
          this.setRangeTo(
            new Date(today.getFullYear(), today.getMonth(), 1),
            today,
          ),
      },
      {
        label: 'Mes pasado',
        apply: () =>
          this.setRangeTo(
            new Date(today.getFullYear(), today.getMonth() - 1, 1),
            new Date(today.getFullYear(), today.getMonth(), 0),
          ),
      },
    ];
  }

  // Las gráficas no caben en un CSV (es texto plano)
  get canCharts(): boolean {
    return this.format !== 'csv';
  }

  // "Lunes 3 may - Domingo 9 may 2026" para mostrar la semana elegida
  get periodText(): string {
    if (!this.pair) return '';
    const last = addDays(this.pair.current.to, -1);
    const sameDay = last.getTime() === this.pair.current.from.getTime();
    return sameDay
      ? fmtDate(this.pair.current.from)
      : `${fmtDate(this.pair.current.from)} - ${fmtDate(last)}`;
  }

  get days(): number {
    if (!this.pair) return 0;
    return Math.round(
      (this.pair.current.to.getTime() - this.pair.current.from.getTime()) /
        86_400_000,
    );
  }

  // =========================
  // Acciones
  // =========================

  setMode(value: string) {
    this.mode = value as PeriodMode;
    this.recalc();
  }

  onDay(event: Event) {
    this.dayValue = (event.target as HTMLInputElement).value;
    this.recalc();
  }

  onWeek(event: Event) {
    this.weekValue = (event.target as HTMLInputElement).value;
    this.recalc();
  }

  onFrom(event: Event) {
    this.fromValue = (event.target as HTMLInputElement).value;
    this.recalc();
  }

  onTo(event: Event) {
    this.toValue = (event.target as HTMLInputElement).value;
    this.recalc();
  }

  setFormat(value: ExportFormat) {
    this.format = value;
  }

  setIncludeCharts(checked: boolean) {
    this.includeCharts = checked;
  }

  confirm() {
    if (!this.pair || this.error || this.exporting) return;
    this.confirmed.emit({
      format: this.format,
      pair: this.pair,
      includeCharts: this.includeCharts && this.canCharts,
    });
  }

  onEmail(event: Event) {
    this.emailTo = (event.target as HTMLInputElement).value;
    this.emailError = '';
  }

  sendByEmail() {
    if (!this.pair || this.error || this.exporting) return;
    if (!isValidEmail(this.emailTo)) {
      this.emailError = EMAIL_ERROR_MESSAGE;
      return;
    }
    this.emitEmail();
  }

  private emitEmail() {
    this.emailed.emit({
      format: this.format,
      pair: this.pair!,
      includeCharts: this.includeCharts && this.canCharts,
      to: this.emailTo.trim(),
    });
  }

  close() {
    if (!this.exporting) this.closed.emit();
  }

  @HostListener('document:keydown.escape')
  onEscape() {
    this.close();
  }

  private setDayTo(d: Date) {
    this.dayValue = toInput(d);
    this.recalc();
  }

  private setWeekTo(d: Date) {
    this.weekValue = toInput(d);
    this.recalc();
  }

  private setRangeTo(from: Date, to: Date) {
    this.fromValue = toInput(from);
    this.toValue = toInput(to);
    this.recalc();
  }

  // Valida lo elegido y calcula el periodo y sus totales para la vista previa
  private recalc() {
    this.pair = null;
    this.totals = null;
    this.error = '';
    const today = startOfDay(new Date());

    if (this.mode === 'day') {
      const d = fromInput(this.dayValue);
      if (!d) this.error = 'Elige una fecha.';
      else if (d > today) this.error = 'La fecha no puede ser futura.';
      else this.pair = customPair('day', d, d);
    } else if (this.mode === 'week') {
      const d = fromInput(this.weekValue);
      if (!d) {
        this.error = 'Elige un día de la semana que quieres descargar.';
      } else {
        const monday = startOfWeek(d);
        if (monday > today) this.error = 'Esa semana todavía no empieza.';
        else this.pair = customPair('week', monday, addDays(monday, 6));
      }
    } else {
      const from = fromInput(this.fromValue);
      const to = fromInput(this.toValue);
      if (!from || !to) this.error = 'Elige la fecha inicial y la final.';
      else if (to < from)
        this.error = 'La fecha final debe ser igual o posterior a la inicial.';
      else if (to > today) this.error = 'La fecha final no puede ser futura.';
      else if ((to.getTime() - from.getTime()) / 86_400_000 + 1 > MAX_DAYS)
        this.error = 'El rango máximo es de 1 año.';
      else this.pair = customPair('custom', from, to);
    }

    if (this.pair) this.totals = totalsFor(this.model, this.pair.current);
  }
}
