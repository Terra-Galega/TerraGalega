import { Component, EventEmitter, Input, Output } from '@angular/core';
import { DecimalPipe } from '@angular/common';
import { CartService, CartItem } from '../../services/cart.service';

@Component({
  selector: 'app-cart',
  imports: [DecimalPipe],
  templateUrl: './cart.component.html',
  styleUrl: './cart.component.scss',
})
export class CartComponent {
  @Input() isOpen = false;
  @Output() closed = new EventEmitter<void>();

  cartItems: CartItem[] = [];

  constructor(private cartService: CartService) {
    this.cartService.items$.subscribe((items) => {
      this.cartItems = items;
    });
  }

  closeCart() {
    this.closed.emit();
  }

  unitPrice(item: CartItem): number {
    return this.cartService.unitPrice(item);
  }

  get total() {
    return this.cartService.total;
  }

  get itemCount() {
    return this.cartService.count;
  }

  removeItem(id: string) {
    this.cartService.removeItem(id);
  }
}
