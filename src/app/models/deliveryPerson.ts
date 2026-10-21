import { Role } from './role';

export interface DeliveryPerson {
  id: number;
  name: string;
  lastName: string;
  email: string;
  password: string;
  role: Role;
  phone: string;
  identification: string;
  available: boolean;
  active: boolean;
}
// Datos que manda el admin al crear: el id, el rol y la disponibilidad los define el back
export type CreateDeliveryPerson = Omit<
  DeliveryPerson,
  'id' | 'role' | 'available'
>;
