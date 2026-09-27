import { Category } from "./category";
export interface AddOn {
  id: number;
  name: string;
  description: string;
  price: number;
  active: boolean;
  categoryIds: number[];
}