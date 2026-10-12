import { Component, OnInit, inject } from '@angular/core';
import { FormBuilder, ReactiveFormsModule } from '@angular/forms';
import { Router } from '@angular/router';
import { NavbarComponent } from '../../components/navbar/navbar.component';
import { FooterComponent } from '../../components/footer/footer.component';
import { PageHeaderComponent } from '../../components/page-header/page-header.component';
import { OrderSummaryComponent } from './components/order-summary/order-summary.component';
import { DeliveryAddressComponent } from './components/delivery-address/delivery-address.component';
import { PaymentFormComponent } from './components/payment-form/payment-form.component';
import { EmptyStateComponent } from '../../components/empty-state/empty-state.component';
import { AuthService } from '../../services/auth.service';
import { CartItem, CartService } from '../../services/cart.service';
import { CheckoutService } from '../../services/checkout.service';
import { CheckoutRequest } from '../../models/checkout';
import { OrderService } from '../../services/order.service';


@Component({
  selector: 'app-checkout',
  imports: [
    ReactiveFormsModule,
    NavbarComponent,
    FooterComponent,
    PageHeaderComponent,
    OrderSummaryComponent,
    DeliveryAddressComponent,
    PaymentFormComponent,
    EmptyStateComponent,
  ],
  templateUrl: './checkout.component.html',
})
export class CheckoutComponent implements OnInit {
  private fb = inject(FormBuilder);
  private router = inject(Router);
  private auth = inject(AuthService);
  private cartService = inject(CartService);
  private checkoutService = inject(CheckoutService);
  private orderService = inject(OrderService);

  clientId: number | null = null;
  clientName = '';

  paying = false;
  addressError = false;
  cardError = '';
  serverError = '';

  form = this.fb.nonNullable.group({
    address: [''],
    cardName: [''],
    cardNumber: [''],
    cardExpiry: [''],
    cardCvv: [''],
  });

  ngOnInit(): void {
    const session = this.auth.session;

    if (!session || session.type !== 'client') {
      this.router.navigate(['/login'], {
        queryParams: { redirect: 'checkout' },
      });
      return;
    }

    this.clientId = session.id;
    this.clientName = session.name;
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

  formatCardNumber(event: Event): void {
    const digits = (event.target as HTMLInputElement).value
      .replace(/\D/g, '')
      .slice(0, 16);

    this.form.controls.cardNumber.setValue(
      digits.replace(/(.{4})/g, '$1 ').trim(),
    );
  }

  formatExpiry(event: Event): void {
    const digits = (event.target as HTMLInputElement).value
      .replace(/\D/g, '')
      .slice(0, 4);

    this.form.controls.cardExpiry.setValue(
      digits.length > 2
        ? `${digits.slice(0, 2)}/${digits.slice(2)}`
        : digits,
    );
  }

  formatCvv(event: Event): void {
    this.form.controls.cardCvv.setValue(
      (event.target as HTMLInputElement).value
        .replace(/\D/g, '')
        .slice(0, 3),
    );
  }

  fillTestData(): void {
    this.form.setValue({
      address: 'Calle 123 #45-67, apto 8',
      cardName: 'Juan García',
      cardNumber: '4111 1111 1111 1111',
      cardExpiry: '12/28',
      cardCvv: '123',
    });
  }

  pay(): void {
    this.addressError = false;
    this.cardError = '';
    this.serverError = '';

    if (this.paying || this.clientId === null || this.items.length === 0) {
      return;
    }

    const { address, cardName, cardNumber, cardExpiry, cardCvv } =
      this.form.getRawValue();

    if (!address.trim()) {
      this.addressError = true;
      return;
    }

    const digits = cardNumber.replace(/\s/g, '');
    const validExpiry = /^(0[1-9]|1[0-2])\/\d{2}$/.test(
      cardExpiry.trim(),
    );

    if (
      !cardName.trim() ||
      digits.length < 13 ||
      !validExpiry ||
      cardCvv.length < 3
    ) {
      this.cardError = 'Revisa los datos de la tarjeta.';
      return;
    }

    const request: CheckoutRequest = {
      address: address.trim(),
      items: this.items.map((item) => ({
        productId: item.product.id,
        quantity: item.quantity,
        addOnIds: item.addOns.map((addOn) => addOn.id),
      })),
    };

    this.paying = true;

    this.checkoutService.checkout(this.clientId, request).subscribe({
      next: () => {
        this.cartService.clear();
        this.orderService.refresh();

        this.router.navigate(['/orders'], {
          queryParams: { paid: true },
        });
      },
      error: (error) => {
        console.error('Error en el checkout:', error);

        this.serverError =
          error?.error?.message ??
          'No se pudo procesar el pedido. Inténtalo de nuevo.';

        this.paying = false;
      },
    });
  }

  cancel(): void {
    const confirmCancel = confirm(
      '¿Seguro que quieres cancelar la compra?\n\nTu carrito se conservará.',
    );

    if (confirmCancel) {
      this.router.navigate(['/menu']);
    }
  }

  goToMenu(): void {
    this.router.navigate(['/menu']);
  }
}