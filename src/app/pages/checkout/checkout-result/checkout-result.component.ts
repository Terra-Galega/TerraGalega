import { TranslatePipe } from '../../../services/translate.pipe';
import { Component, DestroyRef, HostListener, OnInit, inject } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { ActivatedRoute, Router } from '@angular/router';
import { HttpErrorResponse } from '@angular/common/http';
import { exhaustMap, Subscription, timer } from 'rxjs';
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
import { StripeReturnService } from '../../../services/stripe-return.service';
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
  private stripeReturn = inject(StripeReturnService);

  view: View = 'checking';
  payment: Payment | null = null;
  invoice: InvoiceLink | null = null;
  errorMessage = '';
  // true mientras se cancela, para bloquear los botones
  busy = false;

  private orderId = 0;
  private resume = false;
  private polls = 0;
  private poll?: Subscription;

  ngOnInit(): void {
    const session = this.auth.session;
    if (!session || session.type !== 'client') {
      this.router.navigate(['/login'], { queryParams: { redirect: 'orders' } });
      return;
    }

    // Se llegó desde Stripe por el camino normal: ya no hace falta el aviso de "Atrás"
    this.stripeReturn.clear();

    const params = this.route.snapshot.queryParamMap;
    this.orderId = Number(params.get('order'));
    this.resume = params.get('resume') === 'true' || params.get('canceled') === 'true';

    if (!Number.isInteger(this.orderId) || this.orderId <= 0) {
      this.showError('No encontramos el pago que buscas.');
      return;
    }

    this.check();
  }

  // Si se fue otra vez a Stripe ("Volver a pagar") y regresó con "Atrás", el navegador
  // puede mostrar esta página guardada en memoria: se vuelve a consultar el pago.
  @HostListener('window:pageshow', ['$event'])
  onPageShow(event: PageTransitionEvent): void {
    if (event.persisted && this.orderId > 0) {
      this.stripeReturn.clear();
      this.busy = false;
      this.check();
    }
  }

  // Consulta el estado. El back también pregunta a Stripe, así que la
  // respuesta es correcta aunque el webhook todavía no haya llegado.
  check(): void {
    this.view = 'checking';
    this.polls = 0;
    this.poll?.unsubscribe();

    this.poll = timer(0, POLL_MS).pipe(
      exhaustMap(() => this.paymentService.status(this.orderId)),
      takeUntilDestroyed(this.destroyRef),
    ).subscribe({
      next: (payment) => {
        this.polls++;
        this.apply(payment);
      },
      error: (error) => {
        if (this.resume && error instanceof HttpErrorResponse && [403, 404].includes(error.status)) {
          this.paymentService.clearPending(this.orderId);
          this.router.navigate(['/checkout'], { replaceUrl: true });
          return;
        }
        this.showError(getErrorMessage(error, 'No pudimos consultar tu pago.'));
      },
    });
  }

  @HostListener('window:pageshow', ['$event'])
  onPageShow(event: PageTransitionEvent): void {
    if (event.persisted && this.payment?.state === 'PENDING') {
      this.resume = true;
      this.check();
    }
  }

  private apply(payment: Payment): void {
    this.payment = payment;

    if (payment.state === 'PENDING') {
      // Pulsó "volver" en Stripe: no tiene sentido seguir esperando
      if (this.resume && payment.checkoutUrl) {
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
      this.releaseAttempt();
      this.stop('expired');
    } else {
      this.releaseAttempt();
      this.stop('refunded');
    }
  }

  private releaseAttempt(): void {
    if (this.paymentService.pendingOrderId() === this.orderId) {
      this.draftService.draft = { ...this.draftService.draft, paymentRequestId: '', paymentRequestBody: '' };
    }
    this.paymentService.clearPending(this.orderId);
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

    if (this.paymentService.pendingOrderId() === this.orderId) {
      this.cartService.clear();
      this.draftService.reset();
    }
    this.paymentService.clearPending(this.orderId);
    this.orderService.refresh();
    this.couponService.refresh();

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

    this.paymentService.cancelPending(this.orderId).subscribe({
      next: (payment) => {
        this.busy = false;
        this.apply(payment);
        if (payment.state === 'PENDING' && !payment.checkoutUrl) this.check();
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
