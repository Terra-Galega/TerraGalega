import { TranslatePipe } from '../../pipes/translate.pipe';
import {
  Component,
  EventEmitter,
  Input,
  OnChanges,
  Output,
  SimpleChanges,
  inject,
} from '@angular/core';
import { DialogDirective } from '../../directives/dialog.directive';
import { DecimalPipe } from '@angular/common';
import { Router } from '@angular/router';
import { AuthService } from '../../services/auth.service';
import { ToastService } from '../../services/toast.service';
import { DeliveryFeeService } from '../../services/delivery-fee.service';
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
  imports: [DialogDirective, DecimalPipe,ButtonComponent, TranslatePipe],
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

  // Domicilio que se muestra en el total (gratis si es Premium)
  protected delivery = inject(DeliveryFeeService);

  constructor(
    private cartService: CartService,
    private router: Router,
    private auth: AuthService,
    private toast: ToastService,
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
      // Se revisa si es Premium para mostrar el domicilio correcto
      this.delivery.refresh();

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

  // Subtotal de los productos, sin domicilio
  get total() {
    return this.cartService.total;
  }

  // Lo que pagará en total: productos + domicilio. Así el total no cambia de
  // golpe al llegar al último paso del checkout.
  get grandTotal() {
    return this.cartService.total + this.delivery.fee;
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

  readonly maxQuantity = CartService.MAX_QUANTITY;

  // "−" / "+" de una línea: cambia la cantidad sin salir del carrito
  changeQuantity(item: CartItem, delta: number): void {
    this.cartService.setQuantity(item.id, item.quantity + delta);
  }

  // Quita una línea del carrito
  removeItem(id: string) {
    const item = this.cartItems.find((i) => i.id === id);
    this.cartService.removeItem(id);
    this.toast.show('Producto eliminado del carrito', item?.product?.name ?? '');
  }
}