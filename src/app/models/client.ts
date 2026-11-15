import { Role } from './role';
import { Address } from './address';

export interface Client {
  //Datos del cliente identificador unico, nombre...Role:Client, direcciones...
  id: number;
  name: string;
  email: string;
  lastName: string;
  password?: string;
  phone: string;
  role: Role;
  // Las trae el back en GET /clients/{id}; se gestionan por /clients/{id}/addresses
  addresses?: Address[];
  // Suscripción Premium: las calcula el back en cada respuesta de /clients/me.
  // Son opcionales porque al registrarse o editar el perfil el front no las envía.
  subscribed?: boolean;
  subscriptionEndsAt?: string | null;
  daysLeft?: number;
}
//Dto para crear un cliente (omite el id pq se asigna en el back). Al registrarse
//se envía al menos una dirección, solo con su texto (el id lo asigna el back).
export type CreateClient = Omit<Client, 'id' | 'addresses'> & {
  addresses: Pick<Address, 'label'>[];
};
//Dto para mostrarse en admin, omite datos que no se necesitan
export type ClientDTO = Pick<Client, 'id' | 'name' | 'email' | 'phone'>;
