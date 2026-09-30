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
  label: string;
}

export const FEATURES: Feature[] = [
  { key: 'vegetarian', label: 'Vegetariano' },
  { key: 'spicyMild', label: 'Picante suave' },
  { key: 'spicyHot', label: 'Picante fuerte' },
  { key: 'containsNuts', label: 'Contiene nueces' },
  { key: 'containsSeafood', label: 'Contiene mariscos' },
  { key: 'containsGluten', label: 'Contiene gluten' },
];