import { Category } from './category';

export interface AddOn {
  id: number;
  name: string;
  description: string;
  price: number;
  active: boolean;
  categories?: Pick<Category, 'id' | 'name'>[];
}

// Lo que necesita el cliente para elegir adicionales
export interface AddOnOption {
  id: number;
  name: string;
  price: number;
}

export type CreateAddOn = Omit<AddOn, 'id'>;
