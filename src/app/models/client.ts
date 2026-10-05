import { Role } from './role';

export interface Client {
  id: number;
  name: string;
  email: string;
  lastName: string;
  password: string;
  phone: string;
  role: Role;
}
export type CreateClient = Omit<Client, 'id'>;
