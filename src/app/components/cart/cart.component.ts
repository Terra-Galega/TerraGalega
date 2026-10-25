import {
  Component,
  EventEmitter,
  Input,
  OnChanges,
  Output,
  SimpleChanges,
} from '@angular/core';
import { DecimalPipe } from '@angular/common';
import { Router } from '@angular/router';
import { AuthService } from '../../services/auth.service';
import {
  CartService,
  CartItem,
  unavailableMessage,
} from '../../services/cart.service';
import { groupAddOns } from '../../models/order';

@Component({
  selector: 'app-cart',
  imports: [DecimalPipe],
  templateUrl: './cart.component.html',
  styleUrl: './cart.component.scss',
})
export class CartComponent implements OnChanges {
  @Input() isOpen = false;
  @Output() closed = new EventEmitter<void>();

  cartItems: CartItem[] = [];

  // Aviso de productos que se quitaron por no estar disponibles
  removedNotice = '';
  groupAddOns = groupAddOns;

  constructor(
    private cartService: CartService,
    private router: Router,
    private auth: AuthService,
  ) {
    this.cartService.items$.subscribe((items) => {
      this.cartItems = items;
    });
  }

  // Cada vez que se abre el carrito se comprueba que sus productos sigan disponibles
  ngOnChanges(changes: SimpleChanges): void {
    if (!changes['isOpen']) return;

    this.removedNotice = '';

    if (this.isOpen) {
      this.cartService.removeUnavailable().subscribe((removed) => {
        if (removed.length > 0) {
          this.removedNotice = unavailableMessage(removed);
        }
      });
    }
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

  // Cierra el carrito y lleva a la pasarela de pago
  goToCheckout(): void {
    this.closeCart();

    const session = this.auth.session;

    if (!session) {
      this.router.navigate(['/login'], {
        queryParams: { redirect: 'checkout' },
      });
      return;
    }

    if (session.type === 'client') {
      this.router.navigate(['/checkout']);
    }
  }

  removeItem(id: string) {
    this.cartService.removeItem(id);
  }
}
