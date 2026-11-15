import { Component, DestroyRef, OnInit, inject } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { FormBuilder, ReactiveFormsModule } from '@angular/forms';
import { Router } from '@angular/router';
import { NavbarComponent } from '../../components/navbar/navbar.component';
import { FooterComponent } from '../../components/footer/footer.component';
import { PageHeaderComponent } from '../../components/page-header/page-header.component';
import { ButtonComponent } from '../../components/button/button.component';
import { EmptyStateComponent } from '../../components/empty-state/empty-state.component';
import { ModalComponent } from '../../components/modal/modal.component';
import { OrderSummaryComponent } from './components/order-summary/order-summary.component';
import { DeliveryAddressComponent } from './components/delivery-address/delivery-address.component';
import { PaymentFormComponent } from './components/payment-form/payment-form.component';
import { CheckoutCouponComponent } from './components/checkout-coupon/checkout-coupon.component';
import { AuthService } from '../../services/auth.service';
import {
  CartItem,
  CartService,
  unavailableMessage,
} from '../../services/cart.service';
import { CheckoutErrors, emptyErrors } from '../../models/checkout-errors';
import { Address } from '../../models/address';
import { CouponDiscount } from '../../models/coupon';
import { AddressService } from '../../services/address.service';
import { CheckoutDraftService } from '../../services/checkout-draft.service';
import { TutorialCheckoutService } from '../../services/tutorial-checkout.service';


// Paso 1 del checkout: dirección de entrega, cupón y método de pago
@Component({
  selector: 'app-checkout',
  imports: [
    ReactiveFormsModule,
    NavbarComponent,
    FooterComponent,
    PageHeaderComponent,
    ButtonComponent,
    EmptyStateComponent,
    ModalComponent,
    OrderSummaryComponent,
    DeliveryAddressComponent,
    PaymentFormComponent,
    CheckoutCouponComponent,
  ],
  templateUrl: './checkout.component.html',
})
export class CheckoutComponent implements OnInit {
  // Dependencias inyectadas: formulario, navegación, sesión, carrito, borrador del checkout y direcciones
  private fb = inject(FormBuilder);
  private router = inject(Router);
  private auth = inject(AuthService);
  private cartService = inject(CartService);
  private draftService = inject(CheckoutDraftService);
  private destroyRef = inject(DestroyRef);
  private addressService = inject(AddressService);
  // Tramo del checkout del tutorial "hacer un pedido"
  private tutorialCheckout = inject(TutorialCheckoutService);

  // Id del cliente con sesión (null hasta comprobarla); lo necesita el cupón
  clientId: number | null = null;

  // Cupón aplicado ahora mismo (null = ninguno). Arranca en null: si el cliente
  // vuelve del paso 2, el componente del cupón lo revalida y lo emite de nuevo
  coupon: CouponDiscount | null = null;

  // Direcciones guardadas del cliente y si ya terminaron de cargar
  addresses: Address[] = [];
  addressesLoaded = false;

  // Errores por campo; cada uno es '' cuando el campo está bien
  errors: CheckoutErrors = emptyErrors();

  // Una vez que se intentó continuar, los errores se actualizan mientras escribe
  private submitted = false;

  // Visibilidad de los modales de cancelar compra y de error
  showCancelModal = false;
  showErrorModal = false;

  // Textos del modal de cancelación
  confirmTitle = 'Cancelar compra';
  confirmMessage =
    '¿Seguro que quieres cancelar la compra?\n\nTu carrito se conservará.';

  // Textos del modal de error; se rellenan cuando hay que avisar algo (p. ej. carrito actualizado)
  errorTitle = '';
  errorMessage = '';

  // Formulario único que comparten los componentes hijos (dirección y pago).
  // nonNullable evita valores null: los campos vacíos son ''.
  // Las validaciones no van aquí sino en validate(), para controlar cuándo se muestran.
  // No hay campos de tarjeta: con 'card' el cliente paga en la página de Stripe.
  form = this.fb.nonNullable.group({
    address: [''],
    saveAddress: [true],
    addressName: [''],
    method: ['' as 'cash' | 'card' | ''],
  });

  // Al abrir la página: exige sesión de cliente, precarga el formulario y
  // limpia del carrito lo que ya no esté disponible
  ngOnInit(): void {
    // Solo un cliente con sesión puede pagar; si no, va al login y vuelve aquí (redirect)
    const session = this.auth.session;
    if (!session || session.type !== 'client') {
      this.router.navigate(['/login'], {
        queryParams: { redirect: 'checkout' },
      });
      return;
    }

    this.clientId = session.id;

    // Si viene del tutorial "hacer un pedido", sigue con el tramo del checkout
    this.tutorialCheckout.resumeCheckout();

    // Si el cliente vuelve desde el paso 2 (botón Editar) se recupera lo que
    // había escrito. El cupón no va en el formulario: se reaplica aparte.
    const { address, saveAddress, addressName, method } = this.draftService.draft;
    this.form.patchValue({ address, saveAddress, addressName, method });

    // Direcciones guardadas del cliente; si la carga falla, igual puede escribir una
    this.addressService.getAddresses().subscribe({
      next: (addresses) => this.setAddresses(addresses),
      error: (error) => {
        console.error('Error cargando direcciones:', error);
        this.setAddresses([]);
      },
    });

    // Tras el primer intento de continuar, revalida en vivo para que los errores
    // desaparezcan mientras el cliente corrige
    this.form.valueChanges
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe(() => {
        if (this.submitted) {
          this.errors = this.validate();
        }
      });

    // Antes de pagar se quitan los productos que ya no están disponibles
    this.cartService.removeUnavailable().subscribe((removed) => {
      if (removed.length > 0) {
        this.errorTitle = 'Actualizamos tu carrito';
        this.errorMessage = unavailableMessage(removed);
        this.showErrorModal = true;
      }
    });
  }

  // Datos del carrito que se muestran en el resumen (vienen del CartService)
  get items(): CartItem[] {
    return this.cartService.items;
  }

  get total(): number {
    return this.cartService.total;
  }

  // Descuento del cupón aplicado (0 si no hay)
  get discount(): number {
    return this.coupon?.discount ?? 0;
  }

  // Código guardado en el borrador, para reaplicar el cupón al volver del paso 2
  get initialCouponCode(): string {
    return this.draftService.draft.coupon?.code ?? '';
  }

  // Arrow function para poder pasarla como @Input al resumen sin perder el `this`
  unitPrice = (item: CartItem): number => {
    return this.cartService.unitPrice(item);
  };

  // El componente del cupón avisa cuando se aplica, cambia o se quita (null)
  onCouponChange(coupon: CouponDiscount | null): void {
    this.coupon = coupon;
  }

  // Guarda las direcciones y, si aún no hay una elegida, propone la predeterminada
  private setAddresses(addresses: Address[]): void {
    this.addresses = addresses;
    this.addressesLoaded = true;

    if (!this.form.controls.address.value.trim() && addresses.length > 0) {
      const preferred = addresses.find((a) => a.defaultAddress) ?? addresses[0];
      this.form.controls.address.setValue(preferred.label);
    }
  }

  // ¿La dirección ya está en las guardadas? (sin importar mayúsculas, como en el back)
  private isSavedAddress(address: string): boolean {
    const value = address.trim().toLowerCase();
    return this.addresses.some((a) => a.label.trim().toLowerCase() === value);
  }

  // ¿Hay al menos un campo con error?
  private hasErrors(errors: CheckoutErrors): boolean {
    return Object.values(errors).some((message) => message !== '');
  }

  // Devuelve los errores de cada campo ('' = todo bien)
  private validate(): CheckoutErrors {
    // getRawValue() lee todos los campos del formulario
    const v = this.form.getRawValue();
    const errors = emptyErrors();

    const address = v.address.trim();

    if (!address) {
      errors.address = 'Escribe la dirección donde quieres recibir tu pedido.';
    } else if (address.length > this.draftService.addressMax) {
      // Puede pasar si la dirección guardada en el perfil es más larga
      errors.address = 'La dirección no puede superar 120 caracteres.';
    }

    // Nombre de la dirección nueva: solo importa si se va a guardar en el perfil
    const name = v.addressName.trim();
    if (v.saveAddress && address && !this.isSavedAddress(address)) {
      if (name.length > this.draftService.nameMax) {
        errors.addressName = `El nombre no puede superar ${this.draftService.nameMax} caracteres.`;
      } else if (
        name &&
        this.addresses.some(
          (a) => a.name.trim().toLowerCase() === name.toLowerCase(),
        )
      ) {
        errors.addressName = 'Ya tienes una dirección con ese nombre.';
      }
    }

    // Método de pago obligatorio
    if (!v.method) {
      errors.method = 'Elige cómo quieres pagar.';
    }

    return errors;
  }

  // Botón "Continuar": valida, guarda el borrador y pasa al paso 2
  goToReview(): void {
    this.submitted = true;
    this.errors = this.validate();

    if (this.hasErrors(this.errors) || this.items.length === 0) {
      return;
    }

    // Guarda lo elegido en el servicio (no en la URL) para que el paso 2 lo lea
    const v = this.form.getRawValue();
    const address = v.address.trim();

    this.draftService.draft = {
      // Conserva el intento de pago anterior (el paso 2 decide si sirve)
      ...this.draftService.draft,
      ...v,
      address,
      addressName: v.addressName.trim(),
      // Solo se guarda en el perfil si es una dirección nueva y el cliente lo pidió
      saveAddress: v.saveAddress && !this.isSavedAddress(address),
      // Cupón aplicado (null si no hay): el paso 2 lo muestra y lo envía
      coupon: this.coupon,
    };

    this.router.navigate(['/checkout/confirm']);
  }

  // Cancelar compra en dos pasos: cancel() abre el modal, confirmCancel() borra el borrador
  // y vuelve a la carta (el carrito se conserva), closeCancelModal() se arrepiente
  cancel(): void {
    this.showCancelModal = true;
  }

  confirmCancel(): void {
    this.showCancelModal = false;
    this.draftService.reset();
    this.router.navigate(['/menu']);
  }

  closeCancelModal(): void {
    this.showCancelModal = false;
  }

  // Cierra el modal de error y limpia el mensaje
  closeErrorModal(): void {
    this.showErrorModal = false;
    this.errorMessage = '';
  }

  goToMenu(): void {
    this.router.navigate(['/menu']);
  }
}