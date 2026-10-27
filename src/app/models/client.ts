import { Role } from './role';

export interface Client {
  //Datos del cliente identificador unico, nombre...Role:Client, Address...
  id: number;
  name: string;
  email: string;
  lastName: string;
  password?: string;
  phone: string;
  role: Role;
  address: string;
}
//Dto para crar un admin (omite el id pq ese se asigna en el back)
export type CreateClient = Omit<Client, 'id'>;
//Dto para mostrarse en admin, omite datos que no se necesitan
export type ClientDTO = Pick<Client, 'id' | 'name'  | 'email' | 'phone'>;