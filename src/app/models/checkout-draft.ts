// Método de pago elegido en el paso 1
export type PaymentMethod = 'cash' | 'card';

export type DocumentType = 'CC' | 'CE' | 'PAS' | 'NIT';
export type CardKind = 'credit' | 'debit';

// Datos de la tarjeta (simulados). Solo viven en memoria, nunca se guardan.
export interface CardDraft {
  holder: string;
  documentType: DocumentType;
  documentNumber: string;
  number: string;
  expiry: string;
  cvv: string;
  kind: CardKind;
  installments: number;
}

// Lo que el cliente elige en el paso 1 y se muestra en el paso 2
export interface CheckoutDraft {
  address: string;
  method: PaymentMethod | null;
  card: CardDraft;
}

export const emptyCard = (): CardDraft => ({
  holder: '',
  documentType: 'CC',
  documentNumber: '',
  number: '',
  expiry: '',
  cvv: '',
  kind: 'credit',
  installments: 1,
});

export const emptyDraft = (): CheckoutDraft => ({
  address: '',
  method: null,
  card: emptyCard(),
});