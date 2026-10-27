import { Category } from './category';

// Adicional de un plato (por ejemplo, queso extra), con sus datos completos
export interface AddOn {
  id: number;
  name: string;
  description: string;
  price: number;
  // true = disponible para los clientes; false = oculto
  active: boolean;
  // Categorías donde aparece; de cada una solo vienen id y nombre
  categories?: Pick<Category, 'id' | 'name'>[];
}

// Lo que necesita el cliente para elegir adicionales
// (equivale a AddOnOptionDTO del back)
export interface AddOnOption {
  id: number;
  name: string;
  price: number;
}

// Datos para crear un adicional: igual que AddOn pero sin id, porque lo genera el back
export type CreateAddOn = Omit<AddOn, 'id'>;
