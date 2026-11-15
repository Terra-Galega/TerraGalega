import { CelebrationShape } from './celebrations.data';

// Una parte del dibujo de un ícono (el lienzo es de 100 x 100).
// Sin fill ni stroke se rellena con el color actual; con stroke se dibuja solo la línea.
export interface IconPart {
  d: string;
  fill?: string;
  stroke?: number;
}

export const CELEBRATION_ICONS: Record<CelebrationShape, IconPart[]> = {
  spark: [{ d: 'M50 5 58 38 91 50 58 61 50 95 42 61 9 50 42 38Z' }],
  crown: [
    { d: 'M13 68 8 28 32 46 50 17 68 46 92 28 87 68Z' },
    { d: 'M15 77h70v9H15z' },
  ],
  heart: [{ d: 'M50 86 15 52C-1 35 13 11 34 18c8 2 13 8 16 14 3-6 8-12 16-14 21-7 35 17 19 34Z' }],
  petal: [
    { d: 'M18 75C4 32 43 11 82 15c1 40-20 74-64 60Z' },
    { d: 'M20 75c20-10 39-28 61-58', stroke: 4 },
  ],
  flag: [
    { d: 'M17 16v72', stroke: 5 },
    { d: 'M21 21c27-12 43 12 68 0v20c-25 12-41-12-68 0Z', fill: '#fcd116' },
    { d: 'M21 41c27-12 43 12 68 0v12c-25 12-41-12-68 0Z', fill: '#003893' },
    { d: 'M21 53c27-12 43 12 68 0v12c-25 12-41-12-68 0Z', fill: '#ce1126' },
  ],
  ghost: [
    { d: 'M18 82V44a32 32 0 0 1 64 0v38l-13-9-12 9-11-9-13 9-15-9Z' },
    { d: 'M35 47a4 4 0 1 0 8 0a4 4 0 1 0-8 0Z', fill: '#31264b' },
    { d: 'M57 47a4 4 0 1 0 8 0a4 4 0 1 0-8 0Z', fill: '#31264b' },
  ],
  snow: [
    {
      d: 'M50 8v84M8 50h84M20 20l60 60M80 20 20 80M50 8l-9 10m9-10 9 10M50 92l-9-10m9 10 9-10M8 50l10-9M8 50l10 9M92 50l-10-9m10 9-10 9',
      stroke: 5,
    },
  ],
};
