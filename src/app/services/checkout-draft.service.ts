import { Injectable } from '@angular/core';

// Lo que el cliente elige en el paso 1 y se muestra en el paso 2
export interface CheckoutDraft {
  address: string;
  method: 'cash' | 'card' | '';
  holder: string;
  documentType: string;
  documentNumber: string;
  number: string;
  expiry: string;
  cvv: string;
  installments: number;
}

// Guarda en memoria el borrador entre el paso 1 y el paso 2. No usa
// localStorage: si se recarga la página, el cliente vuelve al paso 1.
@Injectable({ providedIn: 'root' })
export class CheckoutDraftService {
  // Máximo que admite la dirección del pedido en la base de datos
  readonly addressMax = 120;

  draft: CheckoutDraft = this.emptyDraft();

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

  reset(): void {
    this.draft = this.emptyDraft();
  }

  // Aviso para el cliente cuando la dirección del pedido es distinta a la de
  // su perfil: al confirmar el pedido, el perfil se actualiza.
  addressNotice(address: string, savedAddress: string): string {
    const current = address.trim();
    const registered = savedAddress.trim();

    if (!current || current === registered) {
      return '';
    }

    return registered
      ? 'Al confirmar tu pedido, esta dirección reemplazará la que tienes registrada en tu perfil. Podrás cambiarla cuando quieras desde «Editar perfil».'
      : 'Al confirmar tu pedido, esta dirección quedará guardada en tu perfil para tus próximos pedidos.';
  }
}