import { Category } from './category';
export interface Product {
  id: number;
  name: string;
  description: string;
  price: number;
  category: Category | null;
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