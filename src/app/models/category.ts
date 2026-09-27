import { AddOn } from "./addOn";

export interface Category {
  id: number;
  name: string;
  description: string;
  addOns: AddOn[];
}