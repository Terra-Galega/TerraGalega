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
import { AuthService } from '../../services/auth.service';
import {
  CartItem,
  CartService,
  unavailableMessage,
} from '../../services/cart.service';
import { CheckoutDraftService } from '../../services/checkout-draft.service';

// Paso 1 del checkout: dirección de entrega y método de pago
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
  ],
  templateUrl: './checkout.component.html',
})
export class CheckoutComponent implements OnInit {
  // Dependencias inyectadas: formulario, navegación, sesión, carrito y borrador del checkout
  private fb = inject(FormBuilder);
  private router = inject(Router);
  private auth = inject(AuthService);
  private cartService = inject(CartService);
  private draftService = inject(CheckoutDraftService);
  private destroyRef = inject(DestroyRef);

  // Dirección guardada en el perfil del cliente
  savedAddress = '';

  // Errores por campo: { address: '...', number: '...' }
  errors: Record<string, string> = {};

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
  form = this.fb.nonNullable.group({
    address: [''],
    method: ['' as 'cash' | 'card' | ''],
    holder: [''],
    documentType: ['CC'],
    documentNumber: [''],
    number: [''],
    expiry: [''],
    cvv: [''],
    installments: [1],
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

    this.savedAddress = session.address ?? '';

    // Si el cliente vuelve desde el paso 2 (botón Editar) se recupera lo que
    // había escrito; si no, la dirección guardada queda lista para editar.
    const draft = this.draftService.draft;
    this.form.patchValue({
      ...draft,
      address: draft.address || this.savedAddress,
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

  // Arrow function para poder pasarla como @Input al resumen sin perder el `this`
  unitPrice = (item: CartItem): number => {
    return this.cartService.unitPrice(item);
  };

  // Aviso de que la dirección del perfil se va a actualizar
  get addressNotice(): string {
    return this.draftService.addressNotice(
      this.form.controls.address.value,
      this.savedAddress,
    );
  }

  // Botón "Usar mi dirección guardada": restaura la dirección del perfil en el formulario
  useSavedAddress(): void {
    this.form.controls.address.setValue(this.savedAddress);
  }

  // Solo para pruebas: llena la tarjeta con datos válidos
  fillTestData(): void {
    this.form.patchValue({
      method: 'card',
      address: this.form.controls.address.value || 'Calle 123 #45-67, apto 8',
      holder: 'Juan García',
      documentType: 'CC',
      documentNumber: '1020304050',
      number: '4111 1111 1111 1111',
      expiry: '12/28',
      cvv: '123',
      installments: 1,
    });
  }

  // Devuelve los errores de cada campo (vacío = todo bien)
  private validate(): Record<string, string> {
    // getRawValue() lee todos los campos del formulario
    const v = this.form.getRawValue();
    const errors: Record<string, string> = {};

    if (!v.address.trim()) {
      errors['address'] =
        'Escribe la dirección donde quieres recibir tu pedido.';
    } else if (v.address.trim().length > this.draftService.addressMax) {
      // Puede pasar si la dirección guardada en el perfil es más larga
      errors['address'] = 'La dirección no puede superar 120 caracteres.';
    }

    // Método de pago obligatorio
    if (!v.method) {
      errors['method'] = 'Elige cómo quieres pagar.';
    }

    // Los datos de tarjeta solo se validan si eligió pagar con tarjeta
    if (v.method === 'card') {
      if (v.holder.trim().length < 3) {
        errors['holder'] = 'Escribe el nombre como aparece en la tarjeta.';
      }
      if (v.documentNumber.trim().length < 5) {
        errors['documentNumber'] = 'Escribe tu número de documento.';
      }
      // Se quitan los espacios del formato antes de contar los 16 dígitos
      if (v.number.replace(/\s/g, '').length !== 16) {
        errors['number'] = 'El número de la tarjeta debe tener 16 dígitos.';
      }
      // Vencimiento con formato MM/AA y mes entre 01 y 12
      if (!/^(0[1-9]|1[0-2])\/\d{2}$/.test(v.expiry)) {
        errors['expiry'] = 'Escribe el vencimiento como MM/AA.';
      }
      if (v.cvv.length !== 3) {
        errors['cvv'] = 'El código de seguridad debe tener 3 dígitos.';
      }
    }

    return errors;
  }

  // Botón "Continuar": valida, guarda el borrador y pasa al paso 2
  goToReview(): void {
    this.submitted = true;
    this.errors = this.validate();

    if (Object.keys(this.errors).length > 0 || this.items.length === 0) {
      return;
    }

    // Guarda lo elegido en el servicio (no en la URL) para que el paso 2 lo lea
    const v = this.form.getRawValue();

    this.draftService.draft = {
      ...v,
      address: v.address.trim(),
    };

    // Con contra entrega no se guarda ningún dato de tarjeta
    if (v.method === 'cash') {
      this.draftService.draft = {
        ...this.draftService.draft,
        holder: '',
        documentNumber: '',
        number: '',
        expiry: '',
        cvv: '',
        installments: 1,
      };
    }

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