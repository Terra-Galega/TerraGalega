import { DecimalPipe } from '@angular/common';
import { Component, OnInit, inject } from '@angular/core';
import { Router } from '@angular/router';
import { NavbarComponent } from '../../../components/navbar/navbar.component';
import { FooterComponent } from '../../../components/footer/footer.component';
import { PageHeaderComponent } from '../../../components/page-header/page-header.component';
import { ButtonComponent } from '../../../components/button/button.component';
import { EmptyStateComponent } from '../../../components/empty-state/empty-state.component';
import { ModalComponent } from '../../../components/modal/modal.component';
import { OrderSummaryComponent } from '../components/order-summary/order-summary.component';
import { AuthService } from '../../../services/auth.service';
import {
  CartItem,
  CartService,
  unavailableMessage,
} from '../../../services/cart.service';
import { AddressService } from '../../../services/address.service';
import { CheckoutService } from '../../../services/checkout.service';
import { CheckoutDraftService } from '../../../services/checkout-draft.service';
import { CouponService } from '../../../services/coupon.service';
import { OrderService } from '../../../services/order.service';
import { PaymentService } from '../../../services/payment.service';
import { CheckoutRequest, DELIVERY_FEE } from '../../../models/checkout';
import { CouponDiscount } from '../../../models/coupon';
import { Payment } from '../../../models/payment';
import { getErrorMessage } from '../../../utils/http-error';

// Paso 2 del checkout: borrador del pedido y confirmación
@Component({
  selector: 'app-checkout-confirm',
  imports: [
    DecimalPipe,
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
  // Dependencias inyectadas: navegación, sesión, carrito, checkout, borrador, pedidos, direcciones y cupones
  private router = inject(Router);
  private auth = inject(AuthService);
  private cartService = inject(CartService);
  private checkoutService = inject(CheckoutService);
  private draftService = inject(CheckoutDraftService);
  private orderService = inject(OrderService);
  private addressService = inject(AddressService);
  private couponService = inject(CouponService);
  private paymentService = inject(PaymentService);

  // Id del cliente logueado; null hasta comprobar la sesión
  clientId: number | null = null;

  // Costo fijo del domicilio, que se suma al total en el resumen
  deliveryFee = DELIVERY_FEE;

  // false mientras se comprueba que el paso 1 esté completo
  ready = false;

  // true mientras se procesa el pago: bloquea botones para evitar doble envío
  paying = false;

  // Mensaje de error del backend mostrado bajo el botón Confirmar
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

    // Si se llegó sin completar el paso 1 (por URL o al recargar la página),
    // se vuelve al inicio del checkout
    if (this.items.length > 0 && (!this.draft.method || !this.draft.address)) {
      this.router.navigate(['/checkout'], { replaceUrl: true });
      return;
    }

    this.ready = true;

    // Antes de confirmar se quitan los productos que ya no están disponibles.
    // Si hay cupón, se revalida: el subtotal pudo cambiar o el cupón dejó de servir
    this.cartService.removeUnavailable().subscribe((removed) => {
      this.revalidateCoupon((couponMessage) => {
        if (removed.length > 0) {
          this.showCartUpdated(removed, couponMessage);
        } else if (couponMessage) {
          this.showNotice('Quitamos tu cupón', couponMessage);
        }
      });
    });
  }

  // Lo elegido en el paso 1 (se lee siempre del servicio, porque el cupón puede cambiar aquí)
  get draft() {
    return this.draftService.draft;
  }

  // Cupón aplicado en el paso 1 (null = sin cupón)
  get coupon(): CouponDiscount | null {
    return this.draft.coupon;
  }

  // Descuento del cupón (0 si no hay)
  get discount(): number {
    return this.coupon?.discount ?? 0;
  }

  // Datos del carrito para el resumen (vienen del CartService)
  get items(): CartItem[] {
    return this.cartService.items;
  }

  get total(): number {
    return this.cartService.total;
  }

  unitPrice = (item: CartItem): number => {
    return this.cartService.unitPrice(item);
  };

  // Vuelve al paso 1 con todo lo escrito conservado
  edit(): void {
    this.router.navigate(['/checkout']);
  }

  // Cambia el cupón del borrador (null = quitarlo)
  private setCoupon(coupon: CouponDiscount | null): void {
    this.draftService.draft = { ...this.draftService.draft, coupon };
  }

  // Vuelve a validar el cupón con el subtotal actual. Si sigue sirviendo, se
  // actualiza su descuento; si no, se quita y done() recibe el motivo.
  // Sin cupón no hace nada y done() recibe ''.
  private revalidateCoupon(done: (couponMessage: string) => void): void {
    const coupon = this.coupon;

    if (!coupon || this.clientId === null) {
      done('');
      return;
    }

    this.couponService
      .validateCoupon(coupon.code, this.total)
      .subscribe({
        next: (result) => {
          this.setCoupon(result);
          done('');
        },
        error: (error) => {
          this.setCoupon(null);
          done(
            `Quitamos el cupón ${coupon.code}. ` +
              getErrorMessage(error, 'Ya no se puede aplicar.'),
          );
        },
      });
  }

  // Abre el modal de error con el título y el texto que se le pasen
  private showNotice(title: string, message: string): void {
    this.errorTitle = title;
    this.errorMessage = message;
    this.showErrorModal = true;
  }

  // Reutiliza el modal de error para avisar qué productos se quitaron del
  // carrito (y, si también se quitó el cupón, por qué)
  private showCartUpdated(removed: string[], couponMessage = ''): void {
    const message = couponMessage
      ? `${unavailableMessage(removed)}\n\n${couponMessage}`
      : unavailableMessage(removed);

    this.showNotice('Actualizamos tu carrito', message);
  }

  // Botón "Confirmar pedido": arma la solicitud y, según el método, crea el
  // pedido en efectivo o lleva al cliente a pagar en Stripe.
  // No hace nada si ya se está pagando, si falta el cliente o si el carrito está vacío.
  confirm(): void {
    if (this.paying || this.clientId === null || this.items.length === 0) {
      return;
    }

    this.serverError = '';

    const address = this.draft.address;
    const saveAddress = this.draft.saveAddress;
    const addressName = this.draft.addressName;

    // Solicitud al backend: dirección y, por cada ítem, producto, cantidad e ids de adicionales.
    // Del cupón solo viaja el código: el back valida y calcula el descuento por su cuenta.
    const request: CheckoutRequest = {
      address,
      items: this.items.map((item) => ({
        productId: item.product.id,
        quantity: item.quantity,
        addOnIds: item.addOns.map((addOn) => addOn.id),
      })),
      couponCode: this.coupon?.code ?? null,
    };

    this.paying = true;

    if (this.draft.method === 'card') {
      this.payWithCard(request);
      return;
    }

    // Contra entrega: el pedido se crea ya
    this.checkoutService
      .checkout(request)
      .subscribe({
        // Éxito: se guarda la dirección nueva (si se pidió), se vacía el carrito,
        // se limpia el borrador y se redirige a /orders (paid=true muestra el aviso verde)
        next: () => {
          if (saveAddress) {
            this.saveNewAddress(address, addressName);
          }

          this.cartService.clear();
          this.orderService.refresh();
          // El cupón usado deja de estar disponible: las listas de cupones se recargan
          this.couponService.refresh();
          this.draftService.reset();

          this.router.navigate(['/orders'], {
            queryParams: { paid: true },
          });
        },
        error: (error) => this.onCheckoutError(error),
      });
  }

  // Tarjeta: el back crea el pedido (oculto hasta que se pague) y una sesión de
  // Stripe; luego el navegador va a la página de pago de Stripe. Al terminar,
  // Stripe vuelve a /checkout/result, que confirma el pago y vacía el carrito.
  private payWithCard(request: CheckoutRequest): void {
    // Mismo carrito = mismo requestId: reintentar tras un fallo de red no crea
    // otro pedido. Si el carrito cambió, se usa un intento nuevo.
    const body = JSON.stringify(request);

    if (!this.draft.paymentRequestId || this.draft.paymentRequestBody !== body) {
      this.draftService.draft = {
        ...this.draftService.draft,
        paymentRequestId: crypto.randomUUID(),
        paymentRequestBody: body,
      };
    }

    this.paymentService
      .start({ requestId: this.draft.paymentRequestId, checkout: request })
      .subscribe({
        next: (payment) => this.goToStripe(payment),
        error: (error) => this.onCheckoutError(error),
      });
  }

  private goToStripe(payment: Payment): void {
    // Ese intento ya se pagó (p. ej. se volvió atrás después de pagar)
    if (payment.state === 'PAID') {
      this.router.navigate(['/checkout/result'], {
        queryParams: { order: payment.orderId },
      });
      return;
    }

    // El intento anterior caducó o se canceló: se prepara uno nuevo
    if (payment.state !== 'PENDING' || !payment.checkoutUrl) {
      this.draftService.draft = {
        ...this.draftService.draft,
        paymentRequestId: '',
        paymentRequestBody: '',
      };
      this.paying = false;
      this.showNotice(
        'El pago anterior caducó',
        'Vuelve a pulsar «Pagar con tarjeta» para generar un pago nuevo.',
      );
      return;
    }

    // Lo que hay que hacer cuando vuelva de Stripe con el pago aprobado
    this.paymentService.savePending({
      orderId: payment.orderId,
      address: this.draft.address,
      saveAddress: this.draft.saveAddress,
      addressName: this.draft.addressName,
    });

    // `paying` queda en true: el botón sigue bloqueado hasta salir de la página
    this.paymentService.redirectToStripe(payment);
  }

  // Fallo al crear el pedido o el pago: se reactiva el botón y se muestra el motivo
  private onCheckoutError(error: unknown): void {
    console.error('Error en el checkout:', error);

    const serverMessage = getErrorMessage(
      error,
      'No se pudo procesar el pedido. Inténtalo de nuevo.',
    );

    this.paying = false;

    // El fallo puede venir de un producto que dejó de estar disponible o
    // del cupón. Se revisan los dos: lo que ya no sirve se quita y se
    // avisa cuál fue el motivo
    this.cartService.removeUnavailable().subscribe((removed) => {
      this.revalidateCoupon((couponMessage) => {
        if (removed.length > 0) {
          this.showCartUpdated(removed, couponMessage);
          return;
        }

        if (couponMessage) {
          this.serverError = couponMessage;
          this.showNotice('Quitamos tu cupón', couponMessage);
          return;
        }

        this.errorTitle = 'No se pudo procesar el pedido';
        this.serverError = serverMessage;
        this.errorMessage = serverMessage;
        this.showErrorModal = true;
      });
    });
  }

  // Agrega la dirección nueva a las del cliente. Sin nombre, el back la llama "Casa".
  // Si falla (por ejemplo, ya estaba guardada), el pedido ya está creado y no se avisa de error.
  private saveNewAddress(address: string, name: string): void {
    this.addressService
      .addAddress({ label: address, name: name || undefined })
      .subscribe({
        error: (error) =>
          console.error('No se pudo guardar la dirección en el perfil:', error),
      });
  }

  // Cancelar compra: no se permite mientras se está procesando el pago
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

  // Cierra el modal de error y limpia los mensajes
  closeErrorModal(): void {
    this.showErrorModal = false;
    this.errorMessage = '';
    this.serverError = '';
  }

  goToMenu(): void {
    this.router.navigate(['/menu']);
  }
}