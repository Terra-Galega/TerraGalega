import { Component, OnInit, inject } from '@angular/core';
import { Router } from '@angular/router';
import { switchMap, timer } from 'rxjs';
import { NavbarComponent } from '../../components/navbar/navbar.component';
import { FooterComponent } from '../../components/footer/footer.component';
import { PageHeaderComponent } from '../../components/page-header/page-header.component';
import { ButtonComponent } from '../../components/button/button.component';
import { EmptyStateComponent } from '../../components/empty-state/empty-state.component';
import {
  ModalComponent,
  getErrorMessage,
} from '../../components/modal/modal.component';
import { OrderSummaryComponent } from './components/order-summary/order-summary.component';
import { AuthService } from '../../services/auth.service';
import {
  CartItem,
  CartService,
  unavailableMessage,
} from '../../services/cart.service';
import { CheckoutService } from '../../services/checkout.service';
import { CheckoutDraftService } from '../../services/checkout-draft.service';
import { ClientService } from '../../services/client.service';
import { OrderService } from '../../services/order.service';
import { CheckoutRequest } from '../../models/checkout';

// Lo que dura el "Procesando pago..." de la pasarela simulada (milisegundos)
const PAYMENT_DELAY_MS = 1500;

// Paso 2 del checkout: borrador del pedido y confirmación
@Component({
  selector: 'app-checkout-confirm',
  imports: [
    NavbarComponent,
    FooterComponent,
    PageHeaderComponent,
    ButtonComponent,
    EmptyStateComponent,
    ModalComponent,
    OrderSummaryComponent,
  ],
  templateUrl: './checkout-confirm.component.html',
})
export class CheckoutConfirmComponent implements OnInit {
  private router = inject(Router);
  private auth = inject(AuthService);
  private cartService = inject(CartService);
  private checkoutService = inject(CheckoutService);
  private draftService = inject(CheckoutDraftService);
  private clientService = inject(ClientService);
  private orderService = inject(OrderService);

  clientId: number | null = null;
  savedAddress = '';

  // Lo elegido en el paso 1
  draft = this.draftService.draft;

  // false mientras se comprueba que el paso 1 esté completo
  ready = false;

  paying = false;
  serverError = '';

  showCancelModal = false;
  showErrorModal = false;

  confirmTitle = 'Cancelar compra';
  confirmMessage =
    '¿Seguro que quieres cancelar la compra?\n\nTu carrito se conservará.';

  errorTitle = 'No se pudo procesar el pedido';
  errorMessage = '';

  ngOnInit(): void {
    const session = this.auth.session;
    if (!session || session.type !== 'client') {
      this.router.navigate(['/login'], {
        queryParams: { redirect: 'checkout' },
      });
      return;
    }

    this.clientId = session.id;
    this.savedAddress = session.address ?? '';

    // Si se llegó sin completar el paso 1 (por URL o al recargar la página),
    // se vuelve al inicio del checkout
    if (this.items.length > 0 && (!this.draft.method || !this.draft.address)) {
      this.router.navigate(['/checkout'], { replaceUrl: true });
      return;
    }

    this.ready = true;

    // Antes de confirmar se quitan los productos que ya no están disponibles
    this.cartService.removeUnavailable().subscribe((removed) => {
      if (removed.length > 0) {
        this.showCartUpdated(removed);
      }
    });
  }

  get items(): CartItem[] {
    return this.cartService.items;
  }

  get total(): number {
    return this.cartService.total;
  }

  unitPrice = (item: CartItem): number => {
    return this.cartService.unitPrice(item);
  };

  get addressNotice(): string {
    return this.draftService.addressNotice(
      this.draft.address,
      this.savedAddress,
    );
  }

  // "•••• 1111"
  get lastDigits(): string {
    return '•••• ' + this.draft.number.slice(-4);
  }

  get installmentsText(): string {
    return this.draft.installments === 1
      ? 'Pago de contado'
      : this.draft.installments + ' cuotas';
  }

  // Vuelve al paso 1 con todo lo escrito conservado
  edit(): void {
    this.router.navigate(['/checkout']);
  }

  private showCartUpdated(removed: string[]): void {
    this.errorTitle = 'Actualizamos tu carrito';
    this.errorMessage = unavailableMessage(removed);
    this.showErrorModal = true;
  }

  confirm(): void {
    if (this.paying || this.clientId === null || this.items.length === 0) {
      return;
    }

    this.serverError = '';

    const clientId = this.clientId;
    const address = this.draft.address;

    const request: CheckoutRequest = {
      address,
      items: this.items.map((item) => ({
        productId: item.product.id,
        quantity: item.quantity,
        addOnIds: item.addOns.map((addOn) => addOn.id),
      })),
    };

    this.paying = true;

    // Con tarjeta se simula lo que tarda la pasarela; contra entrega confirma ya
    const wait = this.draft.method === 'card' ? PAYMENT_DELAY_MS : 0;

    timer(wait)
      .pipe(switchMap(() => this.checkoutService.checkout(clientId, request)))
      .subscribe({
        next: () => {
          // La dirección del pedido pasa a ser la dirección del perfil
          this.saveAddressInProfile(clientId, address);

          this.cartService.clear();
          this.orderService.refresh();
          this.draftService.reset();

          this.router.navigate(['/orders'], {
            queryParams: { paid: true },
          });
        },
        error: (error) => {
          console.error('Error en el checkout:', error);

          const serverMessage = getErrorMessage(
            error,
            'No se pudo procesar el pedido. Inténtalo de nuevo.',
          );

          this.paying = false;

          // Si falló porque un producto dejó de estar disponible, se quita del
          // carrito y se le avisa al cliente cuál fue
          this.cartService.removeUnavailable().subscribe((removed) => {
            if (removed.length > 0) {
              this.showCartUpdated(removed);
              return;
            }

            this.errorTitle = 'No se pudo procesar el pedido';
            this.serverError = serverMessage;
            this.errorMessage = serverMessage;
            this.showErrorModal = true;
          });
        },
      });
  }

  // Si la dirección del pedido es distinta a la del perfil, se actualiza el
  // perfil. Si esto falla, el pedido ya está creado y no se avisa de error.
  private saveAddressInProfile(clientId: number, address: string): void {
    if (address.trim() === this.savedAddress.trim()) {
      return;
    }

    this.clientService
      .getClientById(clientId)
      .pipe(
        switchMap((client) =>
          this.clientService.updateClient({
            ...client,
            address,
            password: '', // la contraseña no se toca desde aquí
          }),
        ),
      )
      .subscribe({
        next: (saved) => this.auth.setSession(saved),
        error: (error) =>
          console.error('No se pudo actualizar la dirección del perfil:', error),
      });
  }

  cancel(): void {
    if (!this.paying) {
      this.showCancelModal = true;
    }
  }

  confirmCancel(): void {
    this.showCancelModal = false;
    this.draftService.reset();
    this.router.navigate(['/menu']);
  }

  closeCancelModal(): void {
    this.showCancelModal = false;
  }

  closeErrorModal(): void {
    this.showErrorModal = false;
    this.errorMessage = '';
    this.serverError = '';
  }

  goToMenu(): void {
    this.router.navigate(['/menu']);
  }
}