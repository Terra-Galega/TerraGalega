import {
  Component,
  ElementRef,
  EventEmitter,
  HostListener,
  Input,
  Output,
  ViewChild,
} from '@angular/core';
import { TranslatePipe } from '../../pipes/translate.pipe';
import {
  BOOTH_SEATS,
  DiningTable,
  FLOOR_TABLES,
  Floor,
} from '../../pages/reservations/reservations-layout';

/** Posición de una silla dentro del SVG de la mesa de celebración (viewBox 84 x 236, en vertical). */
interface Chair {
  x: number;
  y: number;
}

// Ancho del plano en sus propias unidades (viewBox del SVG) y centro aproximado del comedor.
const PLAN_WIDTH = 900;
const DINING_CENTER_X = 500;

// Plano del restaurante (el mismo dibujo de la página de reservas) para el panel del operador.
// Solo dibuja: el padre decide qué mesas tienen reservas, cuál está elegida y cuáles coinciden
// con la búsqueda. Al pulsar una mesa avisa con su número.
@Component({
  selector: 'app-reservation-map',
  imports: [TranslatePipe],
  templateUrl: './reservation-map.component.html',
  // Reutiliza los estilos del plano de la página de reservas y suma los del operador
  styleUrls: [
    '../../pages/reservations/reservations.component.scss',
    './reservation-map.component.scss',
  ],
})
export class ReservationMapComponent {
  /** Piso que se ve. */
  @Input() floor: Floor = 1;
  /** Números de las mesas que tienen reservas activas. */
  @Input() booked: ReadonlySet<number> = new Set();
  /** Mesa elegida (su número) o null. */
  @Input() selected: number | null = null;
  /** Mesas que coinciden con la búsqueda; null = no hay búsqueda y no se atenúa ninguna. */
  @Input() matches: ReadonlySet<number> | null = null;

  @Output() floorChange = new EventEmitter<Floor>();
  @Output() tableSelected = new EventEmitter<number>();

  readonly floors: readonly Floor[] = [1, 2];
  readonly floorNames: Record<Floor, string> = {
    1: 'Piso 1 - Salón principal, barra y cocina',
    2: 'Piso 2 - Salón alto, galería, balcón y salas de celebración',
  };

  tablesOf(level: Floor): readonly DiningTable[] {
    return FLOOR_TABLES[level];
  }

  isBooked(table: DiningTable): boolean {
    return this.booked.has(table.number);
  }

  isMatch(table: DiningTable): boolean {
    return this.matches !== null && this.matches.has(table.number);
  }

  isDim(table: DiningTable): boolean {
    return this.matches !== null && !this.matches.has(table.number);
  }

  changeFloor(floor: Floor): void {
    if (this.floor !== floor) this.floorChange.emit(floor);
  }

  stairLabel(level: Floor): string {
    return level === 1 ? 'Subir a la segunda planta' : 'Bajar a la primera planta';
  }

  /** Descripción de la mesa para lectores de pantalla. */
  labelOf(table: DiningTable): string {
    const name = table.special
      ? `Mesa de celebración - ${table.room}`
      : table.booth !== undefined
        ? `Mesa de arco ${table.number}`
        : `Mesa ${table.number}`;
    const capacity =
      table.booth !== undefined ? `capacidad máxima ${BOOTH_SEATS}` : `${table.seats} puestos`;
    return `${name}, ${capacity}, ${table.area}, ${this.isBooked(table) ? 'con reservas activas' : 'sin reservas'}`;
  }

  /** Sillas de la mesa de celebración: la mitad a la izquierda y el resto a la derecha. */
  chairsOf(table: DiningTable): readonly Chair[] {
    const seats = table.seats;
    const left = Math.ceil(seats / 2);
    const right = seats - left;
    const column = (count: number, x: number): Chair[] =>
      Array.from({ length: count }, (_, i) => ({ x, y: 118 - (count * 21 - 5) / 2 + i * 21 }));
    return [...column(left, 2), ...column(right, 68)];
  }

  // En pantallas estrechas el plano se desplaza en horizontal: se centra en el comedor al
  // aparecer, o en la mesa elegida, y se difumina el borde que tiene más plano por ver.
  private mapEl?: HTMLElement;

  @ViewChild('mapViewport') set mapViewport(ref: ElementRef<HTMLElement> | undefined) {
    const el = ref?.nativeElement;
    if (el === this.mapEl) return;
    this.mapEl = el;
    if (el) setTimeout(() => this.centerMapOn(DINING_CENTER_X));
  }

  /** Centra el plano en una mesa; lo usa el padre al elegirla. */
  centerOnTable(number: number): void {
    const table = [...FLOOR_TABLES[1], ...FLOOR_TABLES[2]].find((t) => t.number === number);
    if (table) this.centerMapOn(table.x);
  }

  private centerMapOn(x: number): void {
    const el = this.mapEl;
    if (!el) return;
    if (el.scrollWidth > el.clientWidth) {
      el.scrollLeft = (x / PLAN_WIDTH) * el.scrollWidth - el.clientWidth / 2;
    }
    this.updateMapEdges();
  }

  @HostListener('window:resize')
  updateMapEdges(): void {
    const el = this.mapEl;
    if (!el) return;
    const max = el.scrollWidth - el.clientWidth;
    el.classList.toggle('more-start', el.scrollLeft > 2);
    el.classList.toggle('more-end', el.scrollLeft < max - 2);
  }
}