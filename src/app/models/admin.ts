import { Role } from './role';

//Clase admin
export interface Admin {
  //Atributos: Identificador unico, nombre... Role: Admin
  id: number;
  name: string;
  lastName: string;
  email: string;
  password?: string;
  role: Role;
}
