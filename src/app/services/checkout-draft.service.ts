import { Injectable } from '@angular/core';
import { CouponDiscount } from '../models/coupon';

// Lo que el cliente elige en el paso 1 y se muestra en el paso 2
export interface CheckoutDraft {
  // Dirección de entrega del pedido
  address: string;
  // true = al confirmar, la dirección se agrega a las guardadas del perfil
  saveAddress: boolean;
  // Forma de pago; vacío mientras el cliente no haya elegido
  method: 'cash' | 'card' | '';
  // Nombre con el que se guarda la dirección nueva (vacío = el back usa "Casa")
  addressName: string;
  // Datos de la tarjeta (simulados). Solo se usan si method es 'card'.
  holder: string;
  documentType: string;
  documentNumber: string;
  number: string;
  expiry: string;
  cvv: string;
  // Número de cuotas (1 = pago de contado)
  installments: number;
  // Cupón aplicado en el paso 1 (null = sin cupón)
  coupon: CouponDiscount | null;
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

  // Devuelve un borrador en blanco, con tipo de documento CC y 1 cuota por defecto.
  emptyDraft(): CheckoutDraft {
    return {
      address: '',
      addressName: '',
      saveAddress: true,
      method: '',
      holder: '',
      documentType: 'CC',
      documentNumber: '',
      number: '',
      expiry: '',
      cvv: '',
      installments: 1,
      coupon: null,
    };
  }

  // Borra el borrador (por ejemplo, al confirmar el pedido), para que los datos de la tarjeta no queden en memoria.
  reset(): void {
    this.draft = this.emptyDraft();
  }
}