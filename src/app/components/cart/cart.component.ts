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
import { ButtonComponent } from '../button/button.component';

// Panel del carrito de compras. Se muestra como una ventana superpuesta
// y es el padre (la barra de navegación) quien decide cuándo abrirla o
// cerrarla mediante isOpen y closed. Los datos y los cálculos vienen de
// CartService; aquí solo se presentan.
@Component({
  selector: 'app-cart',
  imports: [DecimalPipe,ButtonComponent],
  templateUrl: './cart.component.html',
  styleUrl: './cart.component.scss',
})
export class CartComponent implements OnChanges {
  // El padre controla si el carrito está visible
  @Input() isOpen = false;
  // Se emite cuando el cliente pide cerrar el carrito (X, fondo o checkout)
  @Output() closed = new EventEmitter<void>();

  // Copia local del carrito, siempre actualizada por la suscripción
  cartItems: CartItem[] = [];

  // Aviso de productos que se quitaron por no estar disponibles
  removedNotice = '';
  groupAddOns = groupAddOns;

  constructor(
    private cartService: CartService,
    private router: Router,
    private auth: AuthService,
  ) {
    // Mantiene la lista local sincronizada con el carrito del servicio
    this.cartService.items$.subscribe((items) => {
      this.cartItems = items;
    });
  }

  // Cada vez que se abre el carrito se comprueba que sus productos sigan disponibles
  ngOnChanges(changes: SimpleChanges): void {
    if (!changes['isOpen']) return;

    // Al abrir o cerrar se borra el aviso anterior
    this.removedNotice = '';

    if (this.isOpen) {
      // El servicio quita los productos inactivos y devuelve sus nombres
      this.cartService.removeUnavailable().subscribe((removed) => {
        if (removed.length > 0) {
          this.removedNotice = unavailableMessage(removed);
        }
      });
    }
  }

  // Avisa al padre que debe cerrar el carrito
  closeCart() {
    this.closed.emit();
  }

  // Precio de una unidad (producto + adicionales) de una línea del carrito
  unitPrice(item: CartItem): number {
    return this.cartService.unitPrice(item);
  }

  // Total del pedido, sin domicilio
  get total() {
    return this.cartService.total;
  }

  // Número total de unidades en el carrito
  get itemCount() {
    return this.cartService.count;
  }

  // Cierra el carrito y lleva a la pasarela de pago
  goToCheckout(): void {
    this.closeCart();

    const session = this.auth.session;

    // Sin sesión: se envía al login y el parámetro redirect permite
    // volver al checkout después de iniciar sesión
    if (!session) {
      this.router.navigate(['/login'], {
        queryParams: { redirect: 'checkout' },
      });
      return;
    }

    // Solo los clientes pueden pagar; para admin u operador no hace nada
    if (session.type === 'client') {
      this.router.navigate(['/checkout']);
    }
  }

  // Quita una línea del carrito
  removeItem(id: string) {
    this.cartService.removeItem(id);
  }
}