import { Role } from './role';

export interface Operator {
  id: number;
  name: string;
  username: string;
  email: string;
  lastName: string;
  password?: string;
  role: Role;
  active: boolean;
}
export type CreateOperator = Omit<Operator, 'id' | 'password'> & {
  password: string;
};

export { Role };

