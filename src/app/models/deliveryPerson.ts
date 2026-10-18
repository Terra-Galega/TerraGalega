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