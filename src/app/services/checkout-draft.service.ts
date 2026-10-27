import { Injectable } from '@angular/core';

// Lo que el cliente elige en el paso 1 y se muestra en el paso 2
export interface CheckoutDraft {
  // Dirección de entrega del pedido
  address: string;
  // Forma de pago; vacío mientras el cliente no haya elegido
  method: 'cash' | 'card' | '';
  // Datos de la tarjeta (simulados). Solo se usan si method es 'card'.
  holder: string;
  documentType: string;
  documentNumber: string;
  number: string;
  expiry: string;
  cvv: string;
  // Número de cuotas (1 = pago de contado)
  installments: number;
}

// Guarda en memoria el borrador entre el paso 1 y el paso 2. No usa
// localStorage: si se recarga la página, el cliente vuelve al paso 1.
@Injectable({ providedIn: 'root' })
export class CheckoutDraftService {
  // Máximo que admite la dirección del pedido en la base de datos
  readonly addressMax = 120;

  draft: CheckoutDraft = this.emptyDraft();

  // Devuelve un borrador en blanco, con tipo de documento CC y 1 cuota por defecto.
  emptyDraft(): CheckoutDraft {
    return {
      address: '',
      method: '',
      holder: '',
      documentType: 'CC',
      documentNumber: '',
      number: '',
      expiry: '',
      cvv: '',
      installments: 1,
    };
  }

  // Borra el borrador (por ejemplo, al confirmar el pedido), para que los datos de la tarjeta no queden en memoria.
  reset(): void {
    this.draft = this.emptyDraft();
  }

  // Aviso para el cliente cuando la dirección del pedido es distinta a la de
  // su perfil: al confirmar el pedido, el perfil se actualiza.
  // Devuelve '' cuando no hay nada que avisar.
  addressNotice(address: string, savedAddress: string): string {
    const current = address.trim();
    const registered = savedAddress.trim();

    // Sin dirección escrita, o igual a la del perfil: no cambia nada
    if (!current || current === registered) {
      return '';
    }

    // Si ya tenía una dirección se reemplaza; si no, se guarda por primera vez
    return registered
      ? 'Al confirmar tu pedido, esta dirección reemplazará la que tienes registrada en tu perfil. Podrás cambiarla cuando quieras desde «Editar perfil».'
      : 'Al confirmar tu pedido, esta dirección quedará guardada en tu perfil para tus próximos pedidos.';
  }
}