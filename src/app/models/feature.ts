// Características booleanas de un producto (se usan en el modal, filtros y chips)
export type FeatureKey =
  | 'vegetarian'
  | 'spicyMild'
  | 'spicyHot'
  | 'containsNuts'
  | 'containsSeafood'
  | 'containsGluten';

export interface Feature {
  key: FeatureKey;
  label: string; // modal de admin
  chipLabel: string; // chips de las tarjetas
  filterLabel: string; // panel de filtros del menú
}

export const FEATURES: Feature[] = [
  { key: 'vegetarian', label: 'Vegetariano', chipLabel: 'Vegetariano', filterLabel: 'Vegetariano' },
  { key: 'spicyMild', label: 'Picante suave', chipLabel: 'Picante ligero', filterLabel: 'Picante ligero' },
  { key: 'spicyHot', label: 'Picante fuerte', chipLabel: 'Picante intenso', filterLabel: 'Picante intenso' },
  { key: 'containsNuts', label: 'Contiene nueces', chipLabel: 'Nueces', filterLabel: 'Contiene nueces' },
  { key: 'containsSeafood', label: 'Contiene mariscos', chipLabel: 'Mariscos', filterLabel: 'Mariscos' },
  { key: 'containsGluten', label: 'Contiene gluten', chipLabel: 'Gluten', filterLabel: 'Contiene gluten' },
];