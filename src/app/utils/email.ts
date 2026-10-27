export const EMAIL_ERROR_MESSAGE =
  'El correo electrónico no es válido. Debe tener algo antes y después del @.';

// Algo antes del @, un solo @, algo después del @ y sin espacios
export const isValidEmail = (email: string): boolean =>
  /^[^\s@]+@[^\s@]+$/.test(email.trim());
