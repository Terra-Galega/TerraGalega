// Características booleanas de un producto (se usan en el modal, filtros y chips)
export type FeatureKey =
  | 'vegetarian'
  | 'spicyMild'
  | 'spicyHot'
  | 'containsNuts'
  | 'containsSeafood'
  | 'containsGluten';

// Una característica con el texto que se muestra en cada lugar de la app
export interface Feature {
  key: FeatureKey;
  label: string; // modal de admin
  chipLabel: string; // chips de las tarjetas
  filterLabel: string; // panel de filtros del menú
}

// Lista única de características: el admin, las tarjetas y el filtro del menú
// la recorren, así que agregar una aquí la agrega en los tres lugares
export const FEATURES: Feature[] = [
  {
    key: 'vegetarian',
    label: 'Vegetariano',
    chipLabel: 'Vegetariano',
    filterLabel: 'Vegetariano',
  },
  {
    key: 'spicyMild',
    label: 'Picante suave',
    chipLabel: 'Picante ligero',
    filterLabel: 'Picante ligero',
  },
  {
    key: 'spicyHot',
    label: 'Picante fuerte',
    chipLabel: 'Picante intenso',
    filterLabel: 'Picante intenso',
  },
  {
    key: 'containsNuts',
    label: 'Contiene nueces',
    chipLabel: 'Nueces',
    filterLabel: 'Contiene nueces',
  },
  {
    key: 'containsSeafood',
    label: 'Contiene mariscos',
    chipLabel: 'Mariscos',
    filterLabel: 'Mariscos',
  },
  {
    key: 'containsGluten',
    label: 'Contiene gluten',
    chipLabel: 'Gluten',
    filterLabel: 'Contiene gluten',
  },
];
