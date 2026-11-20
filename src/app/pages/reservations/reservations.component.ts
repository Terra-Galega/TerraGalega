import { TranslatePipe } from '../../services/translate.pipe';
import { Component, ElementRef, HostListener, inject, OnInit, ViewChild } from '@angular/core';
import { FooterComponent } from '../../components/footer/footer.component';
import { NavbarComponent } from '../../components/navbar/navbar.component';
import { PageHeaderComponent } from '../../components/page-header/page-header.component';
import { Reservation, ReservationInput, ReservationTheme, RESERVATION_THEMES } from '../../models/reservation';
import { ReservationService } from '../../services/reservation.service';
import { AuthService } from '../../services/auth.service';
import { ReservationGuestComponent } from './components/reservation-guest/reservation-guest.component';
import { ToastService } from '../../services/toast.service';
import { TutorialReservationService } from '../../services/tutorial-reservation.service';
import { getErrorMessage } from '../../utils/http-error';
import {
  BOOTH_SEATS,
  DiningTable,
  Floor,
  FLOOR_TABLES,
  MAX_PARTY,
  SPECIAL_MAX_SEATS,
  SPECIAL_MIN_SEATS,
} from './reservations-layout';

// PÁGINA DE RESERVAS
// Cómo funciona:
// 1. El cliente elige fecha, hora (turnos cada hora y media) y número de comensales.
// 2. El plano muestra las mesas del piso activo (datos en reservations-layout.ts; el back
//    tiene las mismas en la entidad DiningTable) y cada una se pinta según su estado:
//    disponible, reservada (rayada con candado) o con cupo insuficiente. Las reservadas
//    llegan del back (GET /clients/me/reservation/taken), que devuelve sus números.
// 3. Al pulsar una mesa disponible queda seleccionada y su información aparece en "Tu mesa".
//    En las mesas de celebración se pueden añadir o quitar sillas.
// 4. Se escribe el nombre y la temática y se acepta el anticipo. "Reservar y pagar" crea la
//    reserva en el back (queda PENDING y la mesa apartada) y lleva a la pasarela de Stripe.
//    Al volver, /reservations/result confirma el pago (ReservationResultComponent).
// 5. Cada cliente tiene UNA reserva (1 a 1). Si ya la tiene, la página muestra "Tu reserva":
//    - pagada: botones Editar (vuelve al plano con los datos cargados) y Eliminar (sin reembolso);
//    - con el pago pendiente: botones Ir a pagar y Cancelar (no se cobra nada).
// 6. Si el cliente viene del tutorial "hacer una reserva", la página lo retoma
//    (TutorialReservationService) en cuanto sabe qué mostrar.

/** Estado de una mesa para la fecha, hora y grupo elegidos. */
type TableState = 'available' | 'occupied' | 'small';

/** Posición de una silla dentro del SVG de la mesa de celebración (viewBox 84 x 236, en vertical). */
interface Chair {
  x: number;
  y: number;
}

/** Fecha en formato AAAA-MM-DD según la hora local, como la usa el <input type="date">. */
function localDate(date: Date): string {
  const year = date.getFullYear();
  const month = String(date.getMonth() + 1).padStart(2, '0');
  const day = String(date.getDate()).padStart(2, '0');
  return `${year}-${month}-${day}`;
}

// Ancho del plano en sus propias unidades (viewBox del SVG) y centro aproximado del comedor,
// donde se sitúa el plano al abrirlo en móvil.
const PLAN_WIDTH = 900;
const DINING_CENTER_X = 500;

@Component({
  selector: 'app-reservations',
  imports: [NavbarComponent, FooterComponent, PageHeaderComponent, ReservationGuestComponent, TranslatePipe],
  templateUrl: './reservations.component.html',
  styleUrl: './reservations.component.scss',
})
export class ReservationsComponent implements OnInit {
  private reservationService = inject(ReservationService);
  private toast = inject(ToastService);
  private auth = inject(AuthService);
  // Tutorial "hacer una reserva"
  private tutorialReservation = inject(TutorialReservationService);

  /** Sin sesión se muestra la explicación de cómo reservar en lugar del plano. */
  readonly isLoggedIn = this.auth.isLoggedIn;

  // --- Datos fijos de la página ---
  readonly floors: readonly Floor[] = [1, 2];
  readonly floorNames: Record<Floor, string> = {
    1: 'Piso 1 - Salón principal, barra y cocina',
    2: 'Piso 2 - Salón alto, galería, balcón y salas de celebración',
  };
  // Turnos de reserva cada hora y media.
  readonly times = ['12:00', '13:30', '15:00', '16:30', '18:00', '19:30', '21:00'];
  // Opciones de comensales: 1 a 7, donde 7 significa "7 o más".
  readonly parties = Array.from({ length: MAX_PARTY }, (_, i) => i + 1);
  readonly maxSeats = SPECIAL_MAX_SEATS;
  readonly minSeats = SPECIAL_MIN_SEATS;
  readonly boothSeats = BOOTH_SEATS;
  // Anticipo obligatorio de toda reserva (el back cobra este mismo valor).
  readonly deposit = 50000;
  // Hoy: no se permite reservar en fechas anteriores.
  readonly today = localDate(new Date());
  readonly stateLabels: Record<TableState, string> = {
    available: 'Disponible',
    occupied: 'Reservada',
    small: 'Cupo insuficiente',
  };
  readonly themes = RESERVATION_THEMES;

  // --- Estado de la reserva del cliente ---
  reservation: Reservation | null = null; // reserva actual del cliente (null = no tiene)
  loading = true; // cargando la reserva al entrar
  editing = false; // true al editar la reserva existente
  confirmingDelete = false; // muestra el aviso de eliminación
  saving = false; // petición en curso
  reservationName = '';
  theme: ReservationTheme = 'NONE';
  acceptDeposit = false; // aceptar el anticipo (solo al crear)
  /** Números de las mesas reservadas por otros clientes en la fecha y hora elegidas. */
  private taken = new Set<number>();

  // --- Estado del plano ---
  floor: Floor = 1; // piso que se ve en el plano

  // ============================================================
  // RESPONSIVE: plano en móvil
  // En pantallas estrechas el plano no cabe (tiene un ancho mínimo para que las mesas sigan
  // siendo pulsables) y se desplaza en horizontal. Para que se entienda de un vistazo:
  //   - al aparecer, se centra en el comedor y no en la escalera del borde izquierdo;
  //   - al editar una reserva, se centra en la mesa reservada;
  //   - los bordes se difuminan del lado donde queda plano por ver (clases more-start/more-end).
  // En escritorio el plano cabe entero y nada de esto tiene efecto.
  // ============================================================
  private mapEl?: HTMLElement;

  @ViewChild('mapViewport') set mapViewport(ref: ElementRef<HTMLElement> | undefined) {
    const el = ref?.nativeElement;
    if (el === this.mapEl) return;
    this.mapEl = el;
    // Se espera a que el plano tenga su tamaño final antes de medirlo
    if (el) setTimeout(() => this.centerMapOn(this.selected?.x ?? DINING_CENTER_X));
  }

  /** Desplaza el plano para que la coordenada x (en unidades del plano, 0-900) quede al centro. */
  private centerMapOn(x: number): void {
    const el = this.mapEl;
    if (!el) return;
    if (el.scrollWidth > el.clientWidth) {
      el.scrollLeft = (x / PLAN_WIDTH) * el.scrollWidth - el.clientWidth / 2;
    }
    this.updateMapEdges();
  }

  /** Marca en qué lados queda plano por ver, para difuminar ese borde. */
  @HostListener('window:resize')
  updateMapEdges(): void {
    const el = this.mapEl;
    if (!el) return;
    const max = el.scrollWidth - el.clientWidth;
    el.classList.toggle('more-start', el.scrollLeft > 2);
    el.classList.toggle('more-end', el.scrollLeft < max - 2);
  }
  date = this.today;
  time = '19:30';
  party = 2; // número de comensales
  selected: DiningTable | null = null; // mesa elegida, o null si no hay ninguna
  feedback = ''; // mensaje de error al reservar
  /** Sillas elegidas para cada mesa de celebración (por número de mesa). */
  private readonly specialSeats = new Map<number, number>();

  /** Mínimo de sillas de una mesa de celebración: nunca menos que el grupo. */
  get specialMin(): number {
    return Math.max(SPECIAL_MIN_SEATS, this.party);
  }

  /** El botón se activa con mesa disponible, fecha válida, nombre y (al crear) anticipo aceptado. */
  get canReserve(): boolean {
    return (
      !!this.selected &&
      !this.saving &&
      this.date >= this.today &&
      this.stateOf(this.selected) === 'available' &&
      this.reservationName.trim().length > 0 &&
      (this.editing || this.acceptDeposit)
    );
  }

  /** true si la reserva existe pero el anticipo aún no se pagó. */
  get paymentPending(): boolean {
    return this.reservation?.paymentState === 'PENDING';
  }

  ngOnInit(): void {
    if (!this.isLoggedIn) {
      this.loading = false;
      // Tutorial: pedir que entre o se registre
      this.tutorialReservation.resumeReservations('guest');
      return;
    }
    this.reservationService.getReservation().subscribe({
      next: (reservation) => {
        this.reservation = reservation;
        this.loading = false;
        // Tutorial: explicar la reserva que ya tiene (pendiente o confirmada)
        this.tutorialReservation.resumeReservations(
          reservation.paymentState === 'PENDING' ? 'pending' : 'paid',
        );
      },
      // 404 = el cliente aún no tiene reserva: se muestra el plano para crearla
      error: () => {
        this.reservation = null;
        this.loading = false;
        this.loadTaken();
        // Tutorial: guiar por el plano para crearla
        this.tutorialReservation.resumeReservations('new');
      },
    });
  }

  // --- Textos ---

  /** Texto de dinero, ej. "$50.000". */
  money(value: number): string {
    return `$${value.toLocaleString('es-CO')}`;
  }

  themeLabel(theme: ReservationTheme): string {
    return this.themes.find((t) => t.value === theme)?.label ?? theme;
  }

  /** Fecha legible, ej. "sábado, 14 de noviembre de 2026" (se arma sin zona horaria). */
  dateLabel(date: string): string {
    const [y, m, d] = date.split('-').map(Number);
    return new Date(y, m - 1, d).toLocaleDateString('es-CO', {
      weekday: 'long',
      day: 'numeric',
      month: 'long',
      year: 'numeric',
    });
  }

  /** Hora HH:mm a partir de HH:mm:ss. */
  timeLabel(time: string): string {
    return time.slice(0, 5);
  }

  /** Mesa de la reserva con su piso, ej. "Piso 2 - Mesa de celebración - Sala 1". */
  reservationTableLabel(reservation: Reservation): string {
    const found = this.findTable(reservation.tableId);
    return `Piso ${reservation.floor} - ${found ? this.nameOf(found.table) : 'Mesa ' + reservation.tableId}`;
  }

  /** Busca en el plano la mesa con ese número. */
  private findTable(number: number): { table: DiningTable; floor: Floor } | null {
    for (const floor of this.floors) {
      const table = FLOOR_TABLES[floor].find((t) => t.number === number);
      if (table) return { table, floor };
    }
    return null;
  }

  /** Pide al back las mesas ocupadas por otros clientes en la fecha y hora elegidas. */
  private loadTaken(onDone?: () => void): void {
    this.reservationService.getTakenTables(this.date, this.time).subscribe({
      next: (numbers) => {
        this.taken = new Set(numbers);
        onDone?.();
      },
      error: () => {
        this.taken = new Set();
        onDone?.();
      },
    });
  }

  // --- CRUD de la reserva ---

  /** Editar: vuelve al plano con los datos de la reserva cargados y su mesa seleccionada. */
  startEdit(): void {
    const r = this.reservation;
    if (!r) return;
    this.editing = true;
    this.confirmingDelete = false;
    this.feedback = '';
    this.date = r.date;
    this.time = this.timeLabel(r.time);
    this.reservationName = r.name;
    this.theme = r.theme;
    this.party = Math.min(r.seats, MAX_PARTY);
    this.selected = null;
    const found = this.findTable(r.tableId);
    if (found) {
      this.floor = found.floor;
      if (found.table.special) this.specialSeats.set(found.table.number, r.seats);
    }
    this.loadTaken(() => {
      if (found) {
        this.selected = found.table;
        this.centerMapOn(found.table.x);
      }
    });
  }

  cancelEdit(): void {
    this.editing = false;
    this.selected = null;
    this.feedback = '';
  }

  /** Vuelve a la pasarela para pagar el anticipo pendiente. */
  payNow(): void {
    if (!this.reservation) return;
    this.reservationService.redirectToStripe(this.reservation);
  }

  /** Eliminar o cancelar: primero pide confirmar. */
  askDelete(): void {
    this.confirmingDelete = true;
  }

  cancelDelete(): void {
    this.confirmingDelete = false;
  }

  confirmDelete(): void {
    const wasPending = this.paymentPending;
    this.saving = true;
    this.reservationService.deleteReservation().subscribe({
      next: () => {
        this.saving = false;
        this.reservation = null;
        this.confirmingDelete = false;
        this.reservationName = '';
        this.theme = 'NONE';
        this.acceptDeposit = false;
        this.toast.show(
          wasPending ? 'Reserva cancelada' : 'Reserva eliminada',
          wasPending ? 'No se hizo ningún cobro.' : 'El anticipo no se reembolsa.',
        );
        this.loadTaken();
      },
      error: (error) => {
        this.saving = false;
        this.confirmingDelete = false;
        this.toast.error('No se pudo eliminar', getErrorMessage(error));
        // Puede que el pago se haya completado justo antes: se recarga la reserva
        this.reservationService.getReservation().subscribe({
          next: (reservation) => (this.reservation = reservation),
          error: () => (this.reservation = null),
        });
      },
    });
  }

  /**
   * Crear: guarda la reserva (PENDING) y lleva a Stripe a pagar el anticipo.
   * Editar: guarda los cambios (el anticipo ya está pagado).
   */
  reserve(): void {
    const table = this.selected;
    if (!table || !this.canReserve) return;
    const data: ReservationInput = {
      name: this.reservationName.trim(),
      tableId: table.number,
      date: this.date,
      time: this.time,
      // Mesas de celebración: las sillas elegidas; en las demás, los comensales
      seats: table.special ? this.seatsOf(table) : this.party,
      theme: this.theme,
    };
    this.saving = true;
    this.feedback = '';

    if (!this.editing) {
      this.reservationService.createReservation({ ...data, depositAccepted: this.acceptDeposit }).subscribe({
        next: (reservation) => {
          // Se queda "guardando" mientras el navegador va a la pasarela
          this.reservation = reservation;
          this.toast.show('Reserva creada', 'Te llevamos a la pasarela para pagar el anticipo.');
          setTimeout(() => this.reservationService.redirectToStripe(reservation), 1200);
        },
        error: (error) => this.saveFailed(error),
      });
      return;
    }

    this.reservationService.updateReservation(data).subscribe({
      next: (reservation) => {
        this.saving = false;
        this.toast.show(
          'Reserva actualizada',
          `${this.reservationTableLabel(reservation)} a las ${this.timeLabel(reservation.time)}.`,
        );
        this.reservation = reservation;
        this.editing = false;
        this.selected = null;
      },
      error: (error) => this.saveFailed(error),
    });
  }

  private saveFailed(error: unknown): void {
    this.saving = false;
    this.feedback = getErrorMessage(error, 'No se pudo guardar la reserva.');
    this.toast.error(this.editing ? 'No se pudo actualizar la reserva' : 'No se pudo crear la reserva', this.feedback);
    // Si la mesa se ocupó mientras tanto, se refresca el plano
    this.loadTaken();
  }

  // --- Plano ---

  /**
   * Posición de las sillas de una mesa de celebración según los puestos elegidos:
   * la mitad (redondeando hacia arriba) va a la izquierda y el resto a la derecha,
   * centradas a lo largo de la mesa.
   */
  chairsOf(table: DiningTable): readonly Chair[] {
    const seats = this.seatsOf(table);
    const left = Math.ceil(seats / 2);
    const right = seats - left;
    const column = (count: number, x: number): Chair[] =>
      Array.from({ length: count }, (_, i) => ({ x, y: 118 - (count * 21 - 5) / 2 + i * 21 }));
    return [...column(left, 2), ...column(right, 68)];
  }

  /** Mesas de un piso. */
  tablesOf(level: Floor): readonly DiningTable[] {
    return FLOOR_TABLES[level];
  }

  /** Puestos de una mesa; en las de celebración, las sillas que el cliente haya elegido. */
  seatsOf(table: DiningTable): number {
    return table.special ? (this.specialSeats.get(table.number) ?? table.seats) : table.seats;
  }

  /** Nombre que se muestra: "Mesa 112", "Mesa de arco 116" o "Mesa de celebración - Sala 1". */
  nameOf(table: DiningTable): string {
    if (table.special) return `Mesa de celebración - ${table.room}`;
    return table.booth !== undefined ? `Mesa de arco ${table.number}` : `Mesa ${table.number}`;
  }

  /** Ubicación extra que se indica para las mesas de arco. */
  placeOf(table: DiningTable): string {
    return table.booth === undefined ? '' : 'Contra la pared';
  }

  /** Texto del número de comensales: el último valor de la lista significa "7 o más". */
  partyLabel(count: number): string {
    if (count >= MAX_PARTY) return `${MAX_PARTY} o más personas`;
    return count === 1 ? '1 persona' : `${count} personas`;
  }

  /** Texto del aviso que aparece al pasar sobre la escalera. */
  stairLabel(level: Floor): string {
    return level === 1 ? 'Subir a la segunda planta' : 'Bajar a la primera planta';
  }

  /** Descripción completa de la mesa para lectores de pantalla (aria-label del botón). */
  labelOf(table: DiningTable, state: TableState): string {
    const place = this.placeOf(table);
    const capacity = table.booth !== undefined ? `capacidad máxima ${BOOTH_SEATS}` : `${this.seatsOf(table)} puestos`;
    return `${this.nameOf(table)}, ${capacity}, ${table.area}${place ? ', ' + place.toLowerCase() : ''}, ${this.stateLabels[state]}`;
  }

  // --- Cambios en los filtros: cualquier cambio quita la mesa seleccionada ---

  changeFloor(floor: Floor): void {
    if (this.floor === floor) return;
    this.floor = floor;
    this.clearSelection();
  }

  changeDate(date: string): void {
    this.date = date;
    this.clearSelection();
    this.loadTaken();
  }

  changeTime(time: string): void {
    this.time = time;
    this.clearSelection();
    this.loadTaken();
  }

  changeParty(party: string): void {
    this.party = Number(party);
    // Si el grupo crece, las mesas de celebración suben sus sillas para que quepa.
    for (const [number, seats] of this.specialSeats) {
      if (this.party > seats) this.specialSeats.set(number, this.party);
    }
    this.clearSelection();
  }

  /** Botones − y + de la mesa de celebración: suma o resta sillas sin salir del mínimo y el máximo. */
  adjustSeats(table: DiningTable, delta: number): void {
    const seats = Math.min(SPECIAL_MAX_SEATS, Math.max(this.specialMin, this.seatsOf(table) + delta));
    this.specialSeats.set(table.number, seats);
  }

  /**
   * Estado de una mesa para la fecha, hora y grupo elegidos:
   * - occupied: ya reservada por otro cliente (pagada o esperando pago).
   * - small: libre, pero no caben todos los comensales.
   * - available: se puede reservar.
   */
  stateOf(table: DiningTable): TableState {
    if (this.taken.has(table.number)) return 'occupied';
    const capacity = table.special ? SPECIAL_MAX_SEATS : table.seats;
    return capacity < this.party ? 'small' : 'available';
  }

  /** Selecciona una mesa disponible; si ya estaba seleccionada, la deselecciona. */
  selectTable(table: DiningTable): void {
    if (this.stateOf(table) !== 'available') return;
    if (this.selected === table) {
      this.selected = null;
    } else {
      this.selected = table;
      if (table.special) this.specialSeats.set(table.number, Math.max(this.seatsOf(table), this.party));
    }
    this.feedback = '';
  }

  private clearSelection(): void {
    this.selected = null;
    this.feedback = '';
  }
}