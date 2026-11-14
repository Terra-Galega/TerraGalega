import { Component } from '@angular/core';
import { FooterComponent } from '../../components/footer/footer.component';
import { NavbarComponent } from '../../components/navbar/navbar.component';
import { PageHeaderComponent } from '../../components/page-header/page-header.component';
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
// 2. El plano muestra las mesas del piso activo (datos en reservations-layout.ts) y cada una
//    se pinta según su estado: disponible, reservada (rayada con candado) o con cupo insuficiente.
// 3. Al pulsar una mesa disponible queda seleccionada y su información aparece en "Tu mesa".
//    En las mesas de celebración se pueden añadir o quitar sillas.
// 4. "Reservar mesa" guarda la reserva y muestra el mensaje de confirmación.
// Por ahora las reservas solo viven en memoria (se pierden al recargar) y la ocupación es
// simulada; el punto donde conectar la API está indicado en stateOf() y reserve().

/** Estado de una mesa para la fecha, hora y grupo elegidos. */
type TableState = 'available' | 'occupied' | 'small';

/** Posición de una silla dentro del SVG de la mesa de celebración (viewBox 260 x 84). */
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

@Component({
  selector: 'app-reservations',
  imports: [NavbarComponent, FooterComponent, PageHeaderComponent],
  templateUrl: './reservations.component.html',
  styleUrl: './reservations.component.scss',
})
export class ReservationsComponent {
  // --- Datos fijos de la página ---
  readonly floors: readonly Floor[] = [1, 2];
  readonly floorNames: Record<Floor, string> = {
    1: 'Piso 1 - Salón principal, barra y cocina',
    2: 'Piso 2 - Salón alto, balcón y salas de celebración',
  };
  // Turnos de reserva cada hora y media.
  readonly times = ['12:00', '13:30', '15:00', '16:30', '18:00', '19:30', '21:00'];
  // Opciones de comensales: 1 a 7, donde 7 significa "7 o más".
  readonly parties = Array.from({ length: MAX_PARTY }, (_, i) => i + 1);
  readonly maxSeats = SPECIAL_MAX_SEATS;
  readonly minSeats = SPECIAL_MIN_SEATS;
  readonly boothSeats = BOOTH_SEATS;
  // Hoy: no se permite reservar en fechas anteriores.
  readonly today = localDate(new Date());
  readonly stateLabels: Record<TableState, string> = {
    available: 'Disponible',
    occupied: 'Reservada',
    small: 'Cupo insuficiente',
  };

  // --- Estado de la página ---
  floor: Floor = 1; // piso que se ve en el plano
  date = this.today;
  time = '19:30';
  party = 2; // número de comensales
  selected: DiningTable | null = null; // mesa elegida, o null si no hay ninguna
  feedback = ''; // mensaje tras reservar
  /** Sillas elegidas para cada mesa de celebración (por número de mesa). */
  private readonly specialSeats = new Map<number, number>();
  /** Reservas hechas en esta visita a la página, guardadas como "fecha/hora/mesa". */
  private readonly bookings = new Set<string>();

  /** Mínimo de sillas de una mesa de celebración: nunca menos que el grupo. */
  get specialMin(): number {
    return Math.max(SPECIAL_MIN_SEATS, this.party);
  }

  /** El botón "Reservar mesa" solo se activa con una mesa disponible y una fecha válida. */
  get canReserve(): boolean {
    return !!this.selected && this.date >= this.today && this.stateOf(this.selected) === 'available';
  }

  /**
   * Posición de las sillas de una mesa de celebración según los puestos elegidos:
   * la mitad (redondeando hacia arriba) va arriba y el resto abajo, centradas en la mesa.
   */
  chairsOf(table: DiningTable): readonly Chair[] {
    const seats = this.seatsOf(table);
    const top = Math.ceil(seats / 2);
    const bottom = seats - top;
    const row = (count: number, y: number): Chair[] =>
      Array.from({ length: count }, (_, i) => ({ x: 130 - (count * 24 - 6) / 2 + i * 24, y }));
    return [...row(top, 3), ...row(bottom, 67)];
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
  placeOf(table: DiningTable, level: Floor): string {
    if (table.booth === undefined) return '';
    return 'Contra la pared';
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
  labelOf(table: DiningTable, level: Floor, state: TableState): string {
    const place = this.placeOf(table, level);
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
  }

  changeTime(time: string): void {
    this.time = time;
    this.clearSelection();
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
   * - occupied: ya reservada (simulado con una fórmula, o reservada en esta visita).
   * - small: libre, pero no caben todos los comensales.
   * - available: se puede reservar.
   */
  stateOf(table: DiningTable): TableState {
    // Disponibilidad provisional: reemplázala por la respuesta de tu API de reservas.
    const day = Number(this.date.slice(-2));
    const slot = this.times.indexOf(this.time);
    if ((table.number + day + slot) % 5 === 0 || this.bookings.has(this.bookingKey(table))) {
      return 'occupied';
    }
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

  /** Confirma la reserva de la mesa seleccionada. Aquí iría la llamada a la API. */
  reserve(): void {
    const table = this.selected;
    if (!table || !this.canReserve) return;
    this.bookings.add(this.bookingKey(table));
    this.selected = null;
    this.feedback = `Reserva confirmada: ${this.nameOf(table).toLowerCase()} a las ${this.time}.`;
  }

  /** Clave única de una reserva: la misma mesa puede reservarse en otra fecha u hora. */
  private bookingKey(table: DiningTable): string {
    return `${this.date}/${this.time}/${table.number}`;
  }

  private clearSelection(): void {
    this.selected = null;
    this.feedback = '';
  }
}
