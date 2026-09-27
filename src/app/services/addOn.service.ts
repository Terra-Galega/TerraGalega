import { Injectable } from '@angular/core';
import { AddOn } from '../models/addOn';

@Injectable({
  providedIn: 'root',
})
export class AddOnService {

  private addOnArray: AddOn[] = [
    {
      id: 1,
      name: 'Ensalada verde',
      description: 'Adicional para Empanada',
      price: 5000,
      active: true,
      categoryIds: [1],
    },
    {
      id: 2,
      name: 'Extra ajo',
      description: 'Adicional para mariscos',
      price: 2000,
      active: true,
      categoryIds: [2,3],
    },
    {
      id: 3,
      name: 'Salsa de limón',
      description: 'Adicional para mariscos',
      price: 3000,
      active: true,
      categoryIds: [1,2,3],
    },
    {
      id: 4,
      name: 'Pan de millo',
      description: 'Adicional para Lacón',
      price: 3500,
      active: true,
      categoryIds: [1,3],
    },
    {
      id: 5,
      name: 'Cachelos',
      description: 'Adicional para Lacón',
      price: 6000,
      active: true,
      categoryIds: [1,3],
    },
    {
      id: 6,
      name: 'Nata montada',
      description: 'Adicional para Tarta de Santiago',
      price: 3000,
      active: true,
      categoryIds: [4,5],
    },
    {
      id: 7,
      name: 'Helado de vainilla',
      description: 'Adicional para Tarta de Santiago',
      price: 4000,
      active: true,
      categoryIds: [4,5],
    },
    {
      id: 8,
      name: 'Porción de Pan Rústico',
      description:
        'Pan gallego artesanal, ideal para acompañar tus entradas y mojar salsas.',
      price: 3500,
      active: true,
      categoryIds: [2,3],
    },
    {
      id: 9,
      name: 'Extra Salsa Brava',
      description:
        'Una porción extra de nuestra salsa brava ligeramente picante.',
      price: 2500,
      active: true,
      categoryIds: [1,3],
    },
    {
      id: 10,
      name: 'Picos Camperos',
      description:
        'Palitos de pan crujientes típicos, perfectos para picar.',
      price: 2000,
      active: true,
      categoryIds: [1],
    },
    {
      id: 11,
      name: 'Ración de Cachelos',
      description:
        'Patatas cocidas con pimentón, el acompañante perfecto para el pulpo.',
      price: 6000,
      active: true,
      categoryIds: [2,3],
    },
    {
      id: 12,
      name: 'Salsa de Limón y Perejil',
      description:
        'Toque cítrico y fresco extra para tus mariscos y pescados.',
      price: 3000,
      active: true,
      categoryIds: [2,3],
    },
    {
      id: 13,
      name: 'Mayonesa Casera Suave',
      description:
        'Nuestra mayonesa tradicional hecha en casa, sin ajo.',
      price: 2500,
      active: true,
      categoryIds: [2,3],
    },
    {
      id: 14,
      name: 'Ensalada Verde de Guarnición',
      description:
        'Fresca mezcla de lechugas para equilibrar tus carnes.',
      price: 5000,
      active: true,
      categoryIds: [1],
    },
    {
      id: 15,
      name: 'Patatas Panaderas',
      description:
        'Patatas horneadas lentamente a fuego lento, ideales para carnes asadas.',
      price: 6000,
      active: true,
      categoryIds: [3],
    },
    {
      id: 16,
      name: 'Puré de Patatas Trufado',
      description:
        'Cremoso puré de patata con un toque de aceite de trufa blanca.',
      price: 6500,
      active: true,
      categoryIds: [3],
    },
    {
      id: 17,
      name: 'Bola de Helado de Vainilla',
      description:
        'Combina perfecto con torrijas, tartas o postres calientes.',
      price: 4500,
      active: true,
      categoryIds: [4,5],
    },
    {
      id: 18,
      name: 'Nata Montada Extra',
      description:
        'Nata fresca montada al momento, ideal para acompañar dulces.',
      price: 3000,
      active: true,
      categoryIds: [4],
    },
  ];

  getAddOns(): AddOn[] {
    return this.addOnArray;
  }

  getActiveAddOns(): AddOn[] {
    return this.addOnArray.filter(
      (addOn) => addOn.active
    );
  }

  getAddOnById(id: number): AddOn | undefined {
    return this.addOnArray.find(
      (addOn) => addOn.id === id
    );
  }

  getAddOnsByCategoryId(categoryId: number): AddOn[] {
    return this.addOnArray.filter(
      (addOn) => addOn.categoryIds.includes(categoryId)
    );
  }
}