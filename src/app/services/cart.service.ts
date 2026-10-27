import { Injectable, inject } from '@angular/core';
import { BehaviorSubject, Observable, of } from 'rxjs';
import { catchError, map } from 'rxjs/operators';
import { Product } from '../models/product';
import { AuthService, Session } from './auth.service';
import { ProductService } from './product.service';
import { AddOnOption } from '../models/addOn';

export interface CartItem {
  id: string;
  product: Product;
  quantity: number;
  addOns: AddOnOption[];
}

// Mensaje para avisar al cliente de los productos que se quitaron del carrito
export function unavailableMessage(names: string[]): string {
  if (names.length === 1) {
    return `El producto ${names[0]} ya no está disponible y lo quitamos de tu carrito.`;
  }
  const list = names.map((name) => `"${name}"`).join(', ');
  return `Estos productos ya no están disponibles y los quitamos de tu carrito: ${list}.`;
}

// Carrito de quien aún no ha iniciado sesión
const GUEST_KEY = 'terragalega_cart';

// Servicio que administra el carrito de compras. Guarda un carrito por cliente
// en localStorage y mantiene la lista actual en memoria para que los
// componentes se actualicen al instante.
@Injectable({ providedIn: 'root' })
export class CartService {
  // Sesión actual: define de quién es el carrito.
  private auth = inject(AuthService);
  // Se usa para comprobar qué productos siguen activos en el backend.
  private productService = inject(ProductService);

  // Llave de localStorage del carrito que se está usando ahora mismo
  private currentKey: string | null = null;

  // Contenido actual del carrito. BehaviorSubject guarda el último valor,
  // así quien se suscribe recibe de inmediato el estado vigente.
  private itemsSubject = new BehaviorSubject<CartItem[]>([]);

  // Observable de solo lectura para que los componentes escuchen los cambios.
  items$ = this.itemsSubject.asObservable();

  constructor() {
    // Cada vez que cambia la sesión (login, logout) se cambia de carrito.
    // Al suscribirse emite la sesión actual, así que aquí también se
    // carga el carrito inicial.
    this.auth.session$.subscribe((session) => this.switchCart(session));
  }

  // Lectura rápida del contenido actual, sin necesidad de suscribirse.
  get items(): CartItem[] {
    return this.itemsSubject.value;
  }

  // =========================
  // Modificación del carrito
  // =========================

  // Si el mismo producto con los mismos adicionales ya está en el carrito,
  // se suma la cantidad en vez de crear otro registro
  addItem(product: Product, quantity: number, addOns: AddOnOption[]) {
    const id = this.buildId(product, addOns);
    const exists = this.items.some((item) => item.id === id);

    let updated: CartItem[];

    if (exists) {
      // Ya existe: se aumenta la cantidad de esa línea y las demás no cambian
      updated = this.items.map((item) =>
        item.id === id ? { ...item, quantity: item.quantity + quantity } : item,
      );
    } else {
      // Es nueva: se agrega al final
      updated = [...this.items, { id, product, quantity, addOns }];
    }

    this.update(updated);
  }

 
  // Quita del carrito los productos que ya no están disponibles  y devuelve sus nombres.
  // Si el backend falla no se quita nada y se devuelve una lista vacía
  removeUnavailable(): Observable<string[]> {
    if (this.items.length === 0) return of([]);

    return this.productService.getActiveProductIds().pipe(
      map((ids) => {
        const activeIds = new Set(ids);

        const unavailable = this.items.filter(
          (item) => !activeIds.has(item.product.id),
        );
        if (unavailable.length === 0) return [];

        this.update(
          this.items.filter((item) => activeIds.has(item.product.id)),
        );

        return [...new Set(unavailable.map((item) => item.product.name))];
      }),
      catchError(() => of([] as string[])),
    );
  }

  // Elimina una línea completa del carrito
  removeItem(id: string) {
    this.update(this.items.filter((item) => item.id !== id));
  }

  // Vacía el carrito (por ejemplo, después de confirmar un pedido)
  clear() {
    this.update([]);
  }

  // =========================
  // Cálculos
  // =========================

  // Precio de una unidad: producto + sus adicionales
  unitPrice(item: CartItem): number {
    const addOnsTotal = item.addOns.reduce(
      (sum, addOn) => sum + addOn.price,
      0,
    );
    return item.product.price + addOnsTotal;
  }

  // Valor total del carrito (sin domicilio): precio unitario por cantidad de cada línea
  get total(): number {
    return this.items.reduce(
      (sum, item) => sum + this.unitPrice(item) * item.quantity,
      0,
    );
  }

  // Cantidad total de unidades (no de líneas). Es el número que se muestra en el ícono del carrito.
  get count(): number {
    return this.items.reduce((sum, item) => sum + item.quantity, 0);
  }

  // =========================
  // Identificación de líneas
  // =========================

   // Construye el id de una línea a partir del producto y sus adicionales.
  private buildId(product: Product, addOns: AddOnOption[]): string {
    const addOnIds = addOns
      .map((addOn) => addOn.id)
      .sort((a, b) => a - b)
      .join('-');

    return `${product.id}:${addOnIds}`;
  }

  // =========================
  // Carrito por sesión
  // =========================

  // Cada cliente tiene su llave; el resto (invitado, admin, operador,
  // domiciliario) usa la del invitado
  private keyFor(session: Session | null): string {
    return session && session.type === 'client'
      ? `${GUEST_KEY}_client_${session.id}`
      : GUEST_KEY;
  }

  // Cambia al carrito de la sesión actual. Si un invitado inicia sesión
  // como cliente, lo que tenía en su carrito se une al del cliente.
  private switchCart(session: Session | null) {
    const newKey = this.keyFor(session);

    // La sesión se vuelve a emitir al editar el perfil: mismo dueño,
    // no hay nada que cambiar
    if (newKey === this.currentKey) return;

    const previousKey = this.currentKey;
    this.currentKey = newKey;

    // Se carga lo que el nuevo dueño tenía guardado
    let items = this.load(newKey);

    // Invitado que inicia sesión como cliente: se suma lo que ya había
    // agregado y se borra el carrito de invitado para no duplicarlo
    if (previousKey === GUEST_KEY && newKey !== GUEST_KEY) {
      items = this.merge(items, this.items);
      this.remove(GUEST_KEY);
    }

    this.update(items);
  }

  // Suma los productos del segundo carrito al primero
  private merge(base: CartItem[], extra: CartItem[]): CartItem[] {
    // Copia para no modificar el carrito original
    const result = [...base];

    for (const item of extra) {
      const index = result.findIndex((existing) => existing.id === item.id);

      if (index === -1) {
        result.push(item);
      } else {
        result[index] = {
          ...result[index],
          quantity: result[index].quantity + item.quantity,
        };
      }
    }

    return result;
  }

  // =========================
  // Persistencia (localStorage)
  // =========================

  // Punto único de cambio: avisa a los suscriptores y guarda en el navegador
  private update(items: CartItem[]) {
    this.itemsSubject.next(items);
    this.save(items);
  }

  // Guarda el carrito en el navegador bajo la llave del dueño actual
  private save(items: CartItem[]) {
    if (!this.currentKey) return;

    try {
      localStorage.setItem(this.currentKey, JSON.stringify(items));
    } catch {
      // Si el navegador bloquea el almacenamiento, el carrito sigue
      // funcionando en memoria
    }
  }

  // Borra un carrito guardado en el navegador
  private remove(key: string) {
    try {
      localStorage.removeItem(key);
    } catch {
      // Sin almacenamiento no hay nada que borrar
    }
  }

  // Lee un carrito guardado. Si no hay nada, o el contenido está dañado
  // o no tiene el formato esperado, devuelve un carrito vacío
  private load(key: string): CartItem[] {
    try {
      const raw = localStorage.getItem(key);
      if (!raw) return [];

      const parsed = JSON.parse(raw);
      if (!Array.isArray(parsed)) return [];

      // Se descartan las líneas incompletas para que un dato dañado
      // no rompa la pantalla del carrito
      return parsed.filter(
        (item) =>
          item &&
          typeof item.id === 'string' &&
          item.product &&
          typeof item.quantity === 'number' &&
          Array.isArray(item.addOns),
      );
    } catch {
      return [];
    }
  }
}