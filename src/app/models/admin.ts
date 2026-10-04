import { Role } from "./role";

export interface Admin {
  id: number;
  name: string;
  lastName: string;
  email: string;
  password: string;
  role: Role;
}