import { Role } from './role';

export interface Client {
  id: number;
  name: string;
  email: string;
  lastName: string;
  password?: string;
  phone: string;
  role: Role;
  address: string;
}
export type CreateClient = Omit<Client, 'id'>;

export type ClientDTO = Pick<Client, 'id' | 'name'  | 'email' | 'phone'>;