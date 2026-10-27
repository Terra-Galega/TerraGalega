import { Category } from './category';

// Categoría resumida: la que traen los listados de productos
// (solo id y nombre, igual que CategoryRefDTO en el back)
export interface CategoryRef {
  id: number;
  name: string;
}

// Producto tal como llega en los listados (equivale a ProductSummaryDTO del back)
export interface Product {
  id: number;
  name: string;
  description: string;
  price: number;
  // null si el producto no tiene categoría
  category: CategoryRef | null;
  imageUrl: string;
  // true = visible en el menú; false = oculto
  active: boolean;
  // true = aparece en "Favoritos de la casa"
  popular: boolean;
  // Características del plato (filtros del menú)
  vegetarian: boolean;
  spicyMild: boolean;
  spicyHot: boolean;
  // Alérgenos
  containsNuts: boolean;
  containsSeafood: boolean;
  containsGluten: boolean;
}
// Datos para crear un producto: igual que Product pero sin id, porque lo genera el back
export type CreateProduct = Omit<Product, 'id'>;

// Producto del detalle (GET /products/{id}): la categoría trae su descripción
export interface ProductDetail extends Omit<Product, 'category'> {
  category: Pick<Category, 'id' | 'name' | 'description'> | null;
}

// Solo id y nombre, para desplegables de filtro
export interface ProductOption {
  id: number;
  name: string;
}
