import { Injectable } from '@angular/core';
import { CouponDiscount } from '../models/coupon';

// Lo que el cliente elige en el paso 1 y se muestra en el paso 2
export interface CheckoutDraft {
  // Dirección de entrega del pedido
  address: string;
  // true = al confirmar, la dirección se agrega a las guardadas del perfil
  saveAddress: boolean;
  // Forma de pago; vacío mientras el cliente no haya elegido.
  // 'card' = pasarela de Stripe: los datos de la tarjeta se escriben allí.
  method: 'cash' | 'card' | '';
  // Nombre con el que se guarda la dirección nueva (vacío = el back usa "Casa")
  addressName: string;
  // Cupón aplicado en el paso 1 (null = sin cupón)
  coupon: CouponDiscount | null;
  // Identificador del intento de pago con tarjeta. Se reutiliza mientras el
  // carrito no cambie, para que reintentar no cree un pedido duplicado.
  paymentRequestId: string;
  // Carrito (en JSON) con el que se creó ese intento: si cambia, se usa otro id
  paymentRequestBody: string;
}

// Guarda en memoria el borrador entre el paso 1 y el paso 2. No usa
// localStorage: si se recarga la página, el cliente vuelve al paso 1.
@Injectable({ providedIn: 'root' })
export class CheckoutDraftService {

  // Máximo que admite la dirección del pedido en la base de datos
  readonly addressMax = 120;
  // Máximo que admite el nombre de una dirección guardada
  readonly nameMax = 40;

  draft: CheckoutDraft = this.emptyDraft();

  // Devuelve un borrador en blanco
  emptyDraft(): CheckoutDraft {
    return {
      address: '',
      addressName: '',
      saveAddress: true,
      method: '',
      coupon: null,
      paymentRequestId: '',
      paymentRequestBody: '',
    };
  }

  // Borra el borrador (por ejemplo, al confirmar el pedido)
  reset(): void {
    this.draft = this.emptyDraft();
  }
}
