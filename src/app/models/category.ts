import { AddOn } from './addOn';

// Categoría de la carta (por ejemplo, entradas o postres)
export interface Category {
  id: number;
  name: string;
  description: string;
  // El back no lo manda: el admin lo arma en el front cruzando categorías y adicionales
  addOns?: AddOn[];
}

// Datos para crear una categoría: sin id (lo genera el back) y sin adicionales
// (esa relación se maneja desde el adicional)
export type CreateCategory = Omit<Category, 'id' | 'addOns'>;
