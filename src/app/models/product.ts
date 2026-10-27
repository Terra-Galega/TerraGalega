import { Category } from './category';

// Categoría resumida: la que traen los listados de productos
export interface CategoryRef {
  id: number;
  name: string;
}

export interface Product {
  id: number;
  name: string;
  description: string;
  price: number;
  category: CategoryRef | null;
  imageUrl: string;
  active: boolean;
  popular: boolean;
  vegetarian: boolean;
  spicyMild: boolean;
  spicyHot: boolean;
  containsNuts: boolean;
  containsSeafood: boolean;
  containsGluten: boolean;
}
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