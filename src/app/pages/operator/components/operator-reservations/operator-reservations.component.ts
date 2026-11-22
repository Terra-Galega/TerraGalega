import { Component, DestroyRef, ElementRef, ViewChild, inject } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { DatePipe } from '@angular/common';
import { EMPTY, catchError, exhaustMap, filter, timer } from 'rxjs';
import { ReservationService } from '../../../../services/reservation.service';
import {
  OperatorReservation,
  OperatorReservationStatus,
  RESERVATION_THEMES,
} from '../../../../models/reservation';
import { getErrorMessage } from '../../../../utils/http-error';
import { ReservationMapComponent } from '../../../../components/reservation-map/reservation-map.component';
import {
  BOOTH_SEATS,
  DiningTable,
  FLOOR_TABLES,
  Floor,
} from '../../../reservations/reservations-layout';

// Cada cuánto se vuelve a pedir la lista (milisegundos)
const REFRESH_MS = 10_000;

// Ancho por debajo del cual el panel queda debajo del plano (igual que en el .scss)
const STACKED_QUERY = '(max-width: 1180px)';

type StatusTone = 'upcoming' | 'waiting' | 'seated' | 'no-show';

// Texto de cada estado y su tono (los colores están en el .scss, con su versión oscura)
const STATUS_STYLE: Record<OperatorReservationStatus, { label: string; tone: StatusTone }> = {
  UPCOMING: { label: 'Próxima', tone: 'upcoming' },
  WAITING: { label: 'Esperando al cliente', tone: 'waiting' },
  SEATED: { label: 'En la mesa', tone: 'seated' },
  NO_SHOW: { label: 'No llegó', tone: 'no-show' },
};

// Todas las mesas del plano, para buscarlas por número
const ALL_TABLES: readonly DiningTable[] = [...FLOOR_TABLES[1], ...FLOOR_TABLES[2]];

// Minúsculas y sin tildes, para que "jose" encuentre "José"
function normalize(text: string): string {
  return text.normalize('NFD').replace(/[\u0300-\u036f]/g, '').toLowerCase().trim();
}

// Panel del operador: plano de mesas con las reservas activas (pagadas y con el turno sin
// terminar). Al pulsar una mesa se abre, a la derecha (debajo en tablet y móvil), su número y
// sus reservas con la hora. Arriba del plano se puede buscar por número de mesa o por el nombre
// de la reserva / cliente.
@Component({
  selector: 'app-operator-reservations',
  imports: [DatePipe, ReservationMapComponent],
  templateUrl: './operator-reservations.component.html',
  styleUrl: './operator-reservations.component.scss',
})
export class OperatorReservationsComponent {
  private reservationService = inject(ReservationService);

  @ViewChild(ReservationMapComponent) private map?: ReservationMapComponent;
  @ViewChild('tablePanel') private panel?: ElementRef<HTMLElement>;

  reservations: OperatorReservation[] = [];
  // Reservas agrupadas por número de mesa
  byTable = new Map<number, OperatorReservation[]>();
  // Mesas con reservas (para pintarlas en el plano)
  booked: ReadonlySet<number> = new Set();

  loaded = false;
  errorMessage = '';

  floor: Floor = 1;
  selectedNumber: number | null = null;

  search = '';
  // Mesas que coinciden con la búsqueda; null = no hay búsqueda
  matches: ReadonlySet<number> | null = null;

  constructor() {
    timer(0, REFRESH_MS)
      .pipe(
        filter(() => document.visibilityState === 'visible'),
        exhaustMap(() =>
          this.reservationService.getActiveReservations().pipe(
            catchError((error) => {
              this.errorMessage = getErrorMessage(
                error,
                'No se pudieron cargar las reservas.',
              );
              this.loaded = true;
              return EMPTY;
            }),
          ),
        ),
        takeUntilDestroyed(inject(DestroyRef)),
      )
      .subscribe((list) => {
        this.reservations = list;
        this.errorMessage = '';
        this.loaded = true;
        this.index();
        this.applySearch(false);
      });
  }

  // --- Datos derivados ---

  private index() {
    const byTable = new Map<number, OperatorReservation[]>();
    for (const r of this.reservations) {
      byTable.set(r.tableId, [...(byTable.get(r.tableId) ?? []), r]);
    }
    this.byTable = byTable;
    this.booked = new Set(byTable.keys());
  }

  get selectedTable(): DiningTable | null {
    return ALL_TABLES.find((t) => t.number === this.selectedNumber) ?? null;
  }

  get selectedReservations(): OperatorReservation[] {
    return this.selectedNumber === null ? [] : (this.byTable.get(this.selectedNumber) ?? []);
  }

  get matchCount(): number {
    return this.matches?.size ?? 0;
  }

  // --- Acciones ---

  onSearch(event: Event) {
    this.search = (event.target as HTMLInputElement).value;
    this.applySearch(true);
  }

  clearSearch() {
    this.search = '';
    this.applySearch(false);
  }

  setFloor(floor: Floor) {
    this.floor = floor;
  }

  selectTable(number: number) {
    this.selectedNumber = this.selectedNumber === number ? null : number;
    if (this.selectedNumber !== null) {
      this.map?.centerOnTable(this.selectedNumber);
      this.revealPanel();
    }
  }

  closePanel() {
    this.selectedNumber = null;
  }

  // En tablet y móvil el panel queda debajo del plano: se baja hasta él para que el operador
  // vea el detalle sin tener que buscarlo. Sin animación si el usuario pidió reducir movimiento.
  private revealPanel() {
    if (!window.matchMedia(STACKED_QUERY).matches) return;
    const reduce = window.matchMedia('(prefers-reduced-motion: reduce)').matches;
    // Se espera a que Angular pinte el contenido nuevo del panel
    setTimeout(() =>
      this.panel?.nativeElement.scrollIntoView({
        behavior: reduce ? 'auto' : 'smooth',
        block: 'start',
      }),
    );
  }

  // Busca por número de mesa, nombre de la reserva o nombre del cliente
  private applySearch(moveFloor: boolean) {
    const q = normalize(this.search);
    if (!q) {
      this.matches = null;
      return;
    }

    const found = new Set<number>();
    for (const table of ALL_TABLES) {
      if (String(table.number).includes(q)) found.add(table.number);
    }
    for (const r of this.reservations) {
      if (normalize(`${r.name} ${r.clientName}`).includes(q)) found.add(r.tableId);
    }
    this.matches = found;

    // Si no hay coincidencias en el piso que se ve, se pasa al piso de la primera
    if (moveFloor && found.size > 0) {
      const onThisFloor = FLOOR_TABLES[this.floor].some((t) => found.has(t.number));
      if (!onThisFloor) {
        const first = ALL_TABLES.find((t) => found.has(t.number));
        if (first) this.floor = first.number >= 200 ? 2 : 1;
      }
    }
  }

  // --- Textos ---

  tableTitle(table: DiningTable): string {
    if (table.special) return `Mesa de celebración (${table.room})`;
    return table.booth !== undefined ? `Mesa de arco ${table.number}` : `Mesa ${table.number}`;
  }

  tableSeats(table: DiningTable): string {
    return table.booth !== undefined ? `Hasta ${BOOTH_SEATS}` : String(table.seats);
  }

  tableFloor(table: DiningTable): number {
    return table.number >= 200 ? 2 : 1;
  }

  statusStyle(status: OperatorReservationStatus) {
    return STATUS_STYLE[status];
  }

  themeLabel(theme: string): string {
    return RESERVATION_THEMES.find((t) => t.value === theme)?.label ?? theme;
  }

  // "2026-11-22" + "19:30:00" -> Date local, para el pipe de fecha
  at(r: OperatorReservation): Date {
    return new Date(`${r.date}T${r.time}`);
  }

  // "Hoy", "Mañana" o la fecha corta
  dayLabel(date: string): string {
    const d = new Date(`${date}T00:00:00`);
    const today = new Date();
    today.setHours(0, 0, 0, 0);
    const diff = Math.round((d.getTime() - today.getTime()) / 86_400_000);
    if (diff === 0) return 'Hoy';
    if (diff === 1) return 'Mañana';
    const text = d.toLocaleDateString('es-CO', { weekday: 'short', day: 'numeric', month: 'short' });
    return text.charAt(0).toUpperCase() + text.slice(1);
  }
}