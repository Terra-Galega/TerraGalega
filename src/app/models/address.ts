// Dirección guardada de un cliente (llega de /clients/{id}/addresses).
export interface Address {
  id: number;
  // Nombre con el que el cliente la identifica (ej. "Casa", "Oficina")
  name: string;
  // Texto de la dirección
  label: string;
  // true en la dirección predeterminada
  defaultAddress: boolean;
}

// Datos que se envían al crear o editar una dirección (el id no se edita).
export interface AddressInput {
  label: string;
  // Al crear, vacío o ausente = el back asigna "Casa"
  name?: string;
  defaultAddress?: boolean;
}
