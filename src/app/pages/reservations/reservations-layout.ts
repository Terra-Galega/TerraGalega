// DATOS DEL PLANO DE RESERVAS
// Este archivo solo describe el restaurante: qué mesas hay en cada piso, dónde están y
// cuántas personas caben. No tiene lógica; el componente lo lee para dibujar las mesas
// encima del plano y para decidir si una mesa sirve para el grupo elegido.
// Para mover, añadir o quitar una mesa basta con editar FLOOR_TABLES.

/** Pisos del restaurante. */
export type Floor = 1 | 2;

/** Una mesa del plano. */
export interface DiningTable {
  /** Número visible de la mesa (101, 102... en el piso 1; 201, 202... en el piso 2). */
  number: number;
  /**
   * Centro de la mesa en el plano, que mide 900 x 800 unidades (el mismo viewBox del SVG
   * de la arquitectura). El componente lo convierte a porcentaje: left = x / 9, top = y / 8.
   */
  x: number;
  y: number;
  /** Puestos de la mesa. Decide cuántas sillas se dibujan (2, 4 o 6) y si cabe el grupo. */
  seats: number;
  /** Zona del restaurante; se muestra en el resumen "Tu mesa". */
  area: string;
  /**
   * Mesa de arco con el respaldo contra una pared interior (nunca junto a ventanales o balcón).
   * El valor es el giro en grados para que el respaldo quede contra la pared.
   */
  booth?: number;
  /** Giro en grados de una mesa rectangular (por ejemplo, las del balcón en diagonal). */
  rotate?: number;
  /** Mesa de celebración en su propia sala: sus sillas se pueden añadir o quitar. */
  special?: boolean;
  /** Nombre visible de la sala de celebración. */
  room?: string;
}

/** Capacidad máxima de las mesas de arco. */
export const BOOTH_SEATS = 5;
/** Mínimo y máximo de sillas de las mesas de celebración (se ajusta con los botones − y +). */
export const SPECIAL_MIN_SEATS = 7;
export const SPECIAL_MAX_SEATS = 20;
/** La lista de comensales llega hasta este número, que se muestra como "7 o más". */
export const MAX_PARTY = 7;

// Giro de las mesas que siguen la pared en diagonal (la pared forma unos 46° con la horizontal).
const DIAGONAL = 46;

// Mesas de cada piso, con coordenadas sobre el plano de 900 x 800.
// Reglas que sigue la distribución:
// - En cada salón las mesas van de menor a mayor capacidad: 2, 4 y 6 puestos.
// - Junto a los ventanales (piso 1) y en el balcón (piso 2) solo hay mesas rectangulares.
// - Las mesas de arco van contra paredes interiores, sin tapar las puertas de las salas.
// Giros de "booth": 0 respaldo arriba, 90 a la derecha, 180 abajo, 270 a la izquierda,
// 226 contra la pared diagonal de la cocina.
export const FLOOR_TABLES: Record<Floor, readonly DiningTable[]> = {
  1: [
    // Zona de ventanales: solo mesas rectangulares, de 2 y 4 puestos
    { number: 101, x: 200, y: 80, seats: 2, area: 'Zona de ventanales' },
    { number: 102, x: 300, y: 80, seats: 2, area: 'Zona de ventanales' },
    { number: 103, x: 400, y: 80, seats: 2, area: 'Zona de ventanales' },
    { number: 104, x: 500, y: 80, seats: 2, area: 'Zona de ventanales' },
    { number: 105, x: 613, y: 111, seats: 2, area: 'Zona de ventanales', rotate: DIAGONAL },
    { number: 106, x: 764, y: 268, seats: 2, area: 'Zona de ventanales', rotate: DIAGONAL },
    { number: 107, x: 812, y: 385, seats: 4, area: 'Zona de ventanales' },
    { number: 108, x: 812, y: 475, seats: 4, area: 'Zona de ventanales' },
    { number: 109, x: 812, y: 565, seats: 4, area: 'Zona de ventanales' },
    { number: 110, x: 812, y: 655, seats: 4, area: 'Zona de ventanales' },
    { number: 111, x: 812, y: 745, seats: 4, area: 'Zona de ventanales' },
    // Salón principal: mesas de arco contra la pared de la escalera, 2 y 4 puestos
    { number: 112, x: 185, y: 170, seats: BOOTH_SEATS, area: 'Salón principal', booth: 270 },
    { number: 113, x: 300, y: 170, seats: 2, area: 'Salón principal' },
    { number: 114, x: 400, y: 170, seats: 2, area: 'Salón principal' },
    { number: 115, x: 500, y: 170, seats: 2, area: 'Salón principal' },
    { number: 116, x: 185, y: 258, seats: BOOTH_SEATS, area: 'Salón principal', booth: 270 },
    { number: 117, x: 300, y: 258, seats: 4, area: 'Salón principal' },
    { number: 118, x: 400, y: 258, seats: 4, area: 'Salón principal' },
    { number: 119, x: 500, y: 258, seats: 4, area: 'Salón principal' },
    { number: 120, x: 600, y: 258, seats: 4, area: 'Salón principal' },
    // Salón central: 2 puestos, mesa de arco contra la cocina y 4 puestos
    { number: 121, x: 540, y: 365, seats: 2, area: 'Salón central' },
    { number: 122, x: 630, y: 365, seats: 2, area: 'Salón central' },
    { number: 123, x: 720, y: 365, seats: 2, area: 'Salón central' },
    { number: 124, x: 570, y: 434, seats: BOOTH_SEATS, area: 'Salón central', booth: 226 },
    { number: 125, x: 690, y: 455, seats: 4, area: 'Salón central' },
    // Salón lateral: mesas de arco contra los muros y 6 puestos
    { number: 126, x: 645, y: 565, seats: BOOTH_SEATS, area: 'Salón lateral', booth: 270 },
    { number: 127, x: 722, y: 565, seats: 6, area: 'Salón lateral' },
    { number: 128, x: 645, y: 655, seats: BOOTH_SEATS, area: 'Salón lateral', booth: 270 },
    { number: 129, x: 722, y: 655, seats: 6, area: 'Salón lateral' },
    { number: 130, x: 690, y: 735, seats: BOOTH_SEATS, area: 'Salón lateral', booth: 180 },
  ],
  2: [
    // Balcón (terraza exterior): solo mesas rectangulares, de 2 y 4 puestos
    { number: 201, x: 200, y: 80, seats: 2, area: 'Balcón' },
    { number: 202, x: 300, y: 80, seats: 2, area: 'Balcón' },
    { number: 203, x: 400, y: 80, seats: 2, area: 'Balcón' },
    { number: 204, x: 500, y: 80, seats: 2, area: 'Balcón' },
    { number: 205, x: 613, y: 111, seats: 2, area: 'Balcón', rotate: DIAGONAL },
    { number: 206, x: 688, y: 190, seats: 2, area: 'Balcón', rotate: DIAGONAL },
    { number: 207, x: 764, y: 268, seats: 2, area: 'Balcón', rotate: DIAGONAL },
    { number: 208, x: 812, y: 380, seats: 4, area: 'Balcón' },
    { number: 209, x: 812, y: 535, seats: 4, area: 'Balcón' },
    { number: 210, x: 812, y: 625, seats: 4, area: 'Balcón' },
    { number: 211, x: 812, y: 720, seats: 4, area: 'Balcón' },
    // Salón alto: 2 puestos, mesas de arco contra la pared y 4 puestos
    { number: 212, x: 200, y: 170, seats: 2, area: 'Salón alto' },
    { number: 213, x: 300, y: 170, seats: 2, area: 'Salón alto' },
    { number: 214, x: 400, y: 170, seats: 2, area: 'Salón alto' },
    { number: 215, x: 500, y: 170, seats: 2, area: 'Salón alto' },
    { number: 216, x: 200, y: 255, seats: BOOTH_SEATS, area: 'Salón alto', booth: 180 },
    { number: 217, x: 300, y: 255, seats: BOOTH_SEATS, area: 'Salón alto', booth: 180 },
    { number: 218, x: 400, y: 255, seats: BOOTH_SEATS, area: 'Salón alto', booth: 180 },
    { number: 219, x: 535, y: 260, seats: 4, area: 'Salón alto' },
    { number: 220, x: 625, y: 260, seats: 4, area: 'Salón alto' },
    // Galería: cuadrícula uniforme (filas cada 88 unidades). Las de arco van contra la pared
    // de la Sala 2 y contra el muro inferior; se deja libre el paso a la salida del balcón
    // (x 765, y 432-484) y a la puerta de la Sala 2 (x 450, y 572-604).
    { number: 221, x: 495, y: 350, seats: BOOTH_SEATS, area: 'Galería', booth: 270 },
    { number: 222, x: 590, y: 350, seats: 4, area: 'Galería' },
    { number: 223, x: 685, y: 350, seats: 4, area: 'Galería' },
    { number: 224, x: 495, y: 438, seats: BOOTH_SEATS, area: 'Galería', booth: 270 },
    { number: 225, x: 590, y: 438, seats: 4, area: 'Galería' },
    { number: 226, x: 495, y: 526, seats: BOOTH_SEATS, area: 'Galería', booth: 270 },
    { number: 227, x: 590, y: 526, seats: 4, area: 'Galería' },
    { number: 228, x: 690, y: 526, seats: 6, area: 'Galería' },
    { number: 229, x: 645, y: 650, seats: BOOTH_SEATS, area: 'Galería', booth: 270 },
    { number: 230, x: 722, y: 650, seats: 6, area: 'Galería' },
    { number: 231, x: 645, y: 738, seats: BOOTH_SEATS, area: 'Galería', booth: 270 },
    { number: 232, x: 722, y: 738, seats: 6, area: 'Galería' },
    // Salas de celebración: dos salas verticales del mismo tamaño, con la mesa en vertical
    { number: 233, x: 247, y: 455, seats: 15, area: 'Sala de celebración 1', special: true, room: 'Sala 1' },
    { number: 234, x: 382, y: 455, seats: 15, area: 'Sala de celebración 2', special: true, room: 'Sala 2' },
  ],
};
