// Forma que se dibuja en la decoración (el dibujo está en celebration-icons.ts y particles.ts)
export type CelebrationShape =
  | 'spark'
  | 'crown'
  | 'heart'
  | 'petal'
  | 'flag'
  | 'ghost'
  | 'snow';

export interface Celebration {
  id: string;
  // Fecha en formato MM-DD (hora de Bogotá)
  date: string;
  name: string;
  message: string;
  shape: CelebrationShape;
  // Colores de las figuras que caen
  colors: string[];
  // Color principal de la placa, el brillo y los íconos
  accent: string;
  // true: las figuras caen todo el rato (la nieve); si no, solo caen unos segundos
  continuous?: boolean;
}

// Para agregar una festividad basta con añadir una entrada aquí (y su forma si es nueva)
export const CELEBRATIONS: Celebration[] = [
  {
    id: 'new-year',
    date: '01-01',
    name: 'Año Nuevo',
    message: 'Un nuevo año para compartir alrededor de la mesa.',
    shape: 'spark',
    colors: ['#f8d778', '#fff5d5', '#7cd4eb'],
    accent: '#f8d778',
  },
  {
    id: 'kings',
    date: '01-06',
    name: 'Reyes Magos',
    message: 'Una noche de estrellas, coronas y buenos deseos.',
    shape: 'crown',
    colors: ['#f6d779', '#fff2bb', '#dca8f5'],
    accent: '#f6d779',
  },
  {
    id: 'valentine',
    date: '02-14',
    name: 'San Valentín',
    message: 'Hay momentos que saben mejor si se comparten.',
    shape: 'heart',
    colors: ['#ff8bab', '#ffd3de', '#f4b6c6'],
    accent: '#ff8bab',
  },
  {
    id: 'sant-jordi',
    date: '04-23',
    name: 'Sant Jordi',
    message: 'Una rosa, un libro y una historia para recordar.',
    shape: 'petal',
    colors: ['#ed7285', '#ffb9bd', '#f7d4a5'],
    accent: '#ed7285',
  },
  {
    id: 'colombia',
    date: '07-20',
    name: 'Independencia de Colombia',
    message: 'Celebramos los colores y las historias de Colombia.',
    shape: 'flag',
    colors: ['#fcd116', '#003893', '#ce1126'],
    accent: '#fcd116',
  },
  {
    id: 'halloween',
    date: '10-31',
    name: 'Halloween',
    message: 'Una noche de apariciones y sorpresas en Terra Galega.',
    shape: 'ghost',
    colors: ['#f5f0e8', '#d8c3fa', '#ffc77c'],
    accent: '#d8c3fa',
  },
  {
    id: 'christmas',
    date: '12-25',
    name: 'Navidad',
    message: 'La magia de Navidad también se comparte en la mesa.',
    shape: 'snow',
    colors: ['#fffefa', '#d4efff', '#b5dce8'],
    accent: '#d4efff',
    continuous: true,
  },
];
