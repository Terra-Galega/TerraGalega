import { Injectable } from '@angular/core';
import { BehaviorSubject } from 'rxjs';
import { Product } from '../models/product';
import { AddOn } from '../models/addOn';

export interface CartItem {
  id: string;
  product: Product;
  quantity: number;
  addOns: AddOn[];
}

@Injectable({ providedIn: 'root' })
export class CartService {
  // El carrito vive solo en memoria: si se recarga la página, se vacía
  private itemsSubject = new BehaviorSubject<CartItem[]>([]);

  items$ = this.itemsSubject.asObservable();

  get items(): CartItem[] {
    return this.itemsSubject.value;
  }

  // Si el mismo producto con los mismos adicionales ya está en el carrito,
  // se suma la cantidad en vez de crear otro registro
  addItem(product: Product, quantity: number, addOns: AddOn[]) {
    const id = this.buildId(product, addOns);
    const exists = this.items.some((item) => item.id === id);

    let updated: CartItem[];

    if (exists) {
      updated = this.items.map((item) =>
        item.id === id ? { ...item, quantity: item.quantity + quantity } : item,
      );
    } else {
      updated = [...this.items, { id, product, quantity, addOns }];
    }

    this.itemsSubject.next(updated);
  }

  removeItem(id: string) {
    this.itemsSubject.next(this.items.filter((item) => item.id !== id));
  }

  clear() {
    this.itemsSubject.next([]);
  }

  // Precio de una unidad: producto + sus adicionales
  unitPrice(item: CartItem): number {
    const addOnsTotal = item.addOns.reduce(
      (sum, addOn) => sum + addOn.price,
      0,
    );
    return item.product.price + addOnsTotal;
  }

  get total(): number {
    return this.items.reduce(
      (sum, item) => sum + this.unitPrice(item) * item.quantity,
      0,
    );
  }

  get count(): number {
    return this.items.reduce((sum, item) => sum + item.quantity, 0);
  }

  private buildId(product: Product, addOns: AddOn[]): string {
    const addOnIds = addOns
      .map((addOn) => addOn.id)
      .sort((a, b) => a - b)
      .join('-');

    return `${product.id}:${addOnIds}`;
  }
}
