// Mensaje de error de cada campo del checkout ('' = sin error).
// La tarjeta no se valida aquí: se escribe en la página de Stripe.
export interface CheckoutErrors {
  address: string;
  addressName: string;
  method: string;
}

// Errores en blanco: ningún campo tiene error
export function emptyErrors(): CheckoutErrors {
  return {
    address: '',
    addressName: '',
    method: '',
  };
}
