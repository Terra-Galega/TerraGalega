import { TranslatePipe } from '../../../services/translate.pipe';
import { Component, DestroyRef, OnInit, inject } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { ActivatedRoute, Router } from '@angular/router';
import { Subscription, timer } from 'rxjs';
import { NavbarComponent } from '../../../components/navbar/navbar.component';
import { FooterComponent } from '../../../components/footer/footer.component';
import { PageHeaderComponent } from '../../../components/page-header/page-header.component';
import { ButtonComponent } from '../../../components/button/button.component';
import { AuthService } from '../../../services/auth.service';
import { CartService } from '../../../services/cart.service';
import { AddressService } from '../../../services/address.service';
import { CheckoutDraftService } from '../../../services/checkout-draft.service';
import { CouponService } from '../../../services/coupon.service';
import { OrderService } from '../../../services/order.service';
import { PaymentService } from '../../../services/payment.service';
import { Payment, InvoiceLink } from '../../../models/payment';
import { getErrorMessage } from '../../../utils/http-error';

// Cada cuánto se pregunta el estado mientras Stripe confirma el pago
const POLL_MS = 2000;
// Cuántas veces se pregunta antes de dejar que el cliente lo intente a mano
const MAX_POLLS = 15;

// Lo que se le muestra al cliente
type View =
  | 'checking' // consultando el pago
  | 'abandoned' // volvió de Stripe sin pagar (la sesión sigue abierta)
  | 'waiting' // pagó pero Stripe aún no lo confirma
  | 'expired' // el pago caducó o se canceló
  | 'refunded'
  | 'paid' // se canceló un pedido ya pagado
  | 'error';

// Página a la que vuelve Stripe: /checkout/result?order=ID[&canceled=true].
// Confirma el pago con el back y, si se aprobó, termina el pedido.
@Component({
  selector: 'app-checkout-result',
  imports: [
    NavbarComponent,
    FooterComponent,
    PageHeaderComponent,
    ButtonComponent,
    TranslatePipe],
  templateUrl: './checkout-result.component.html',
})
export class CheckoutResultComponent implements OnInit {
  private route = inject(ActivatedRoute);
  private router = inject(Router);
  private auth = inject(AuthService);
  private destroyRef = inject(DestroyRef);
  private cartService = inject(CartService);
  private addressService = inject(AddressService);
  private draftService = inject(CheckoutDraftService);
  private couponService = inject(CouponService);
  private orderService = inject(OrderService);
  private paymentService = inject(PaymentService);

  view: View = 'checking';
  payment: Payment | null = null;
  invoice: InvoiceLink | null = null;
  errorMessage = '';
  // true mientras se cancela, para bloquear los botones
  busy = false;

  private orderId = 0;
  private canceled = false;
  private polls = 0;
  private poll?: Subscription;

  ngOnInit(): void {
    const session = this.auth.session;
    if (!session || session.type !== 'client') {
      this.router.navigate(['/login'], { queryParams: { redirect: 'orders' } });
      return;
    }

    const params = this.route.snapshot.queryParamMap;
    this.orderId = Number(params.get('order'));
    this.canceled = params.get('canceled') === 'true';

    if (!Number.isInteger(this.orderId) || this.orderId <= 0) {
      this.showError('No encontramos el pago que buscas.');
      return;
    }

    this.check();
  }

  // Consulta el estado. El back también pregunta a Stripe, así que la
  // respuesta es correcta aunque el webhook todavía no haya llegado.
  check(): void {
    this.view = 'checking';
    this.polls = 0;
    this.poll?.unsubscribe();

    this.poll = timer(0, POLL_MS)
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe(() => {
        this.polls++;

        this.paymentService.status(this.orderId).subscribe({
          next: (payment) => this.apply(payment),
          error: (error) => {
            this.poll?.unsubscribe();
            this.showError(
              getErrorMessage(error, 'No pudimos consultar tu pago.'),
            );
          },
        });
      });
  }

  private apply(payment: Payment): void {
    this.payment = payment;

    if (payment.state === 'PENDING') {
      // Pulsó "volver" en Stripe: no tiene sentido seguir esperando
      if (this.canceled) {
        this.stop('abandoned');
      } else if (this.polls >= MAX_POLLS) {
        this.stop('waiting');
      }
      return;
    }

    if (payment.state === 'PAID') {
      this.poll?.unsubscribe();
      this.finish();
    } else if (payment.state === 'EXPIRED') {
      this.stop('expired');
    } else {
      this.stop('refunded');
    }
  }

  private stop(view: View): void {
    this.poll?.unsubscribe();
    this.view = view;
  }

  // Pago aprobado: lo mismo que hacía el checkout en efectivo al terminar
  private finish(): void {
    const pending = this.paymentService.getPending(this.orderId);

    if (pending?.saveAddress) {
      this.addressService
        .addAddress({
          label: pending.address,
          name: pending.addressName || undefined,
        })
        .subscribe({
          error: (error) =>
            console.error(
              'No se pudo guardar la dirección en el perfil:',
              error,
            ),
        });
    }

    this.paymentService.clearPending();
    this.cartService.clear();
    this.orderService.refresh();
    this.couponService.refresh();
    this.draftService.reset();

    // En vez de saltar a /orders, se queda aquí para mostrar el QR de la factura
    this.view = 'paid';
    this.paymentService.invoiceLink(this.orderId).subscribe({
      next: (link) => (this.invoice = link),
      error: () => (this.invoice = null),
    });
  }

  // Vuelve a la página de pago de Stripe (la sesión sigue abierta)
  payAgain(): void {
    if (this.payment) {
      this.paymentService.redirectToStripe(this.payment);
    }
  }

  // Cancela el pedido sin pagar. El carrito se conserva.
  cancelPayment(): void {
    if (this.busy) return;
    this.busy = true;

    this.paymentService.cancel(this.orderId).subscribe({
      next: (payment) => {
        this.busy = false;
        this.paymentService.clearPending();
        this.apply(payment);

        // Si justo se había pagado, apply() ya terminó el pedido
        if (payment.state === 'EXPIRED') {
          this.router.navigate(['/checkout'], { replaceUrl: true });
        }
      },
      error: (error) => {
        this.busy = false;
        this.showError(getErrorMessage(error, 'No pudimos cancelar el pago.'));
      },
    });
  }

  goToCheckout(): void {
    this.router.navigate(['/checkout']);
  }

  goToOrders(): void {
    this.router.navigate(['/orders']);
  }

  private showError(message: string): void {
    this.errorMessage = message;
    this.view = 'error';
  }
}
