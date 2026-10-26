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
  private fb = inject(FormBuilder);
  private router = inject(Router);
  private auth = inject(AuthService);
  private cartService = inject(CartService);
  private draftService = inject(CheckoutDraftService);
  private destroyRef = inject(DestroyRef);

  savedAddress = '';

  // Errores por campo: { address: '...', number: '...' }
  errors: Record<string, string> = {};

  // Una vez que se intentó continuar, los errores se actualizan mientras escribe
  private submitted = false;

  showCancelModal = false;
  showErrorModal = false;

  confirmTitle = 'Cancelar compra';
  confirmMessage =
    '¿Seguro que quieres cancelar la compra?\n\nTu carrito se conservará.';

  errorTitle = '';
  errorMessage = '';

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

  ngOnInit(): void {
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

  get items(): CartItem[] {
    return this.cartService.items;
  }

  get total(): number {
    return this.cartService.total;
  }

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
    const v = this.form.getRawValue();
    const errors: Record<string, string> = {};

    if (!v.address.trim()) {
      errors['address'] =
        'Escribe la dirección donde quieres recibir tu pedido.';
    } else if (v.address.trim().length > this.draftService.addressMax) {
      // Puede pasar si la dirección guardada en el perfil es más larga
      errors['address'] = 'La dirección no puede superar 120 caracteres.';
    }

    if (!v.method) {
      errors['method'] = 'Elige cómo quieres pagar.';
    }

    if (v.method === 'card') {
      if (v.holder.trim().length < 3) {
        errors['holder'] = 'Escribe el nombre como aparece en la tarjeta.';
      }
      if (v.documentNumber.trim().length < 5) {
        errors['documentNumber'] = 'Escribe tu número de documento.';
      }
      if (v.number.replace(/\s/g, '').length !== 16) {
        errors['number'] = 'El número de la tarjeta debe tener 16 dígitos.';
      }
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

  closeErrorModal(): void {
    this.showErrorModal = false;
    this.errorMessage = '';
  }

  goToMenu(): void {
    this.router.navigate(['/menu']);
  }
}