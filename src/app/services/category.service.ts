import { Injectable, inject } from '@angular/core';
import { Category } from '../models/category';
import { AddOnService } from './addOn.service';

@Injectable({
  providedIn: 'root',
})
export class CategoryService {
  private addOnService = inject(AddOnService);

  constructor() {
    this.assignAddOnsToCategories();
  }

  getCategories(): Category[] {
    return this.categoryArray;
  }

  getCategoryById(id: number): Category | undefined {
    return this.categoryArray.find((category) => category.id === id);
  }

  private categoryArray: Category[] = [
    {
      id: 1,
      name: 'Entradas',
      description: "Pequeños platos gallegos pensados para abrir el apetito antes del plato principal.",
      addOns: [],
    },
    {
      id: 2,
      name: 'Mariscos',
      description: "Lo mejor de la costa gallega: mariscos frescos preparados con recetas tradicionales.",
      addOns: [],
    },
    {
      id: 3,
      name: 'Carnes',
      description: "Cortes a la brasa y guisos tradicionales de la cocina gallega.",
      addOns: [],
    },
    {
      id: 4,
      name: 'Postres',
      description: "Dulces clásicos de Galicia para cerrar la comida con sabor a tierra.",
      addOns: [],
    },
    {
      id: 5,
      name: 'Bebidas',
      description: "Mixología, vinos y bebidas premium seleccionadas para elevar tu experiencia gastronómica.",
      addOns: [],
    },
  ];

  private assignAddOnsToCategories(): void {
    this.categoryArray.forEach((category) => {
      category.addOns = this.addOnService.getAddOnsByCategoryId(category.id);
    });
  }
}
