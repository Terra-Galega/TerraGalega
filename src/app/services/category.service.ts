import { Injectable } from '@angular/core';
import { Category } from '../models/category';

@Injectable({
  providedIn: 'root',
})
export class CategoryService {

  constructor() {}

  getCategories() {
    return this.categoryArray;
  }

  getCategoryById(id: number) {
    return this.categoryArray.find((category) => category.id === id);
  }

  private categoryArray: Category[] = [
    {
      id: 1,
      name: 'Entradas',
      description: 'Platos para comenzar la comida',
    },
    {
      id: 2,
      name: 'Mariscos',
      description: 'Platos de mariscos y pescados',
    },
    {
      id: 3,
      name: 'Carnes',
      description: 'Platos tradicionales de carne',
    },
    {
      id: 4,
      name: 'Postres',
      description: 'Postres tradicionales españoles',
    },
    {
      id: 5,
      name: 'Bebidas',
      description: 'Bebidas tradicionales',
    },
  ];
}