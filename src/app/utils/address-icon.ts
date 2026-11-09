// Ícono que representa una dirección según el nombre que le puso el cliente
export type AddressIconKind =
  | 'home'
  | 'work'
  | 'building'
  | 'family'
  | 'school'
  | 'pin'
  | 'plus';

// Elige el ícono buscando palabras clave en el nombre ("Casa", "Oficina"...).
// Si no reconoce ninguna, usa el pin de ubicación.
export function addressIconKind(name: string): AddressIconKind {
  const text = name.trim().toLowerCase();
  const has = (...words: string[]) => words.some((w) => text.includes(w));

  if (has('casa', 'hogar', 'home')) return 'home';
  if (has('oficina', 'trabajo', 'work', 'empresa')) return 'work';
  if (has('apart', 'apto', 'edificio', 'conjunto')) return 'building';
  if (has('mam', 'pap', 'famil', 'abuel', 'herman', 'novi', 'pareja', 'amig'))
    return 'family';
  if (has('universidad', 'colegio', 'escuela', 'estudio', 'school', 'campus'))
    return 'school';
  return 'pin';
}
