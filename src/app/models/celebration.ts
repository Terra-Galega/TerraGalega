// Formas ligeras para las partículas que caen (particles.ts).
export type CelebrationShape =
  | 'spark'
  | 'crown'
  | 'heart'
  | 'petal'
  | 'flag'
  | 'ghost'
  | 'snow';

// Ilustraciones decorativas: independientes de las partículas de fondo.
export type CelebrationIcon = CelebrationShape
  | 'fireworks' | 'gift' | 'love-letter' | 'rose' | 'book'
  | 'vase' | 'pumpkin' | 'santa';

export interface Celebration {
  id: string;
  // Fecha en formato MM-DD (hora de Bogotá)
  date: string;
  name: string;
  message: string;
  // Texto amplio de la tarjeta de Novedades; Mi cuenta conserva el mensaje breve.
  announcementMessage: string;
  shape: CelebrationShape;
  // Pareja de símbolos: su composición depende de la festividad en la escena.
  decorations: readonly [CelebrationIcon, CelebrationIcon];
  // Colores de las figuras que caen
  colors: string[];
  // Color principal de la placa, el brillo y los íconos
  accent: string;
  // true: las figuras caen todo el rato (la nieve); si no, solo caen unos segundos
  continuous?: boolean;
}

