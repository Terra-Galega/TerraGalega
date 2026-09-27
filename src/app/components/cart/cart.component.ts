import { Component, EventEmitter, Input, Output } from '@angular/core';
import { DecimalPipe } from '@angular/common';

@Component({
  selector: 'app-cart',
  imports: [DecimalPipe],
  templateUrl: './cart.component.html',
  styleUrl: './cart.component.scss'
})
export class CartComponent {
  @Input() isOpen = false;
  @Output() closed = new EventEmitter<void>();

  cartItems: any[] = [];

  closeCart() {
    this.closed.emit();
  }

  get total() {
    return this.cartItems.reduce(
      (sum, item) => sum + item.price * item.quantity,
      0
    );
  }

  get itemCount() {
    return this.cartItems.reduce(
      (sum, item) => sum + item.quantity,
      0
    );
  }

  removeItem(id: number) {
    this.cartItems = this.cartItems.filter(item => item.id !== id);
  }
}