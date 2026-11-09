// Mensaje de error de cada campo del checkout ('' = sin error)
export interface CheckoutErrors {
  address: string;
  addressName: string;
  method: string;
  holder: string;
  documentNumber: string;
  number: string;
  expiry: string;
  cvv: string;
}

// Errores en blanco: ningún campo tiene error
export function emptyErrors(): CheckoutErrors {
  return {
    address: '',
    addressName: '',
    method: '',
    holder: '',
    documentNumber: '',
    number: '',
    expiry: '',
    cvv: '',
  };
}