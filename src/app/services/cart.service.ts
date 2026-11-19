import { API_URL } from '../api-url';
import { Injectable, inject } from '@angular/core';
import { HttpClient, HttpContext } from '@angular/common/http';
import { SKIP_BUSY } from '../interceptors/busy.interceptor';
import {
  BehaviorSubject,
  EMPTY,
  Observable,
  fromEvent,
  interval,
  merge,
  of,
} from 'rxjs';
import { catchError, filter, map } from 'rxjs/operators';
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
// Carrito compartido con la app (vive en Postgres)
// El carrito se actualiza primero en pantalla y se guarda en segundo plano:
// esas peticiones no deben deshabilitar los botones de la página (ver BusyService).
const BACKGROUND = { context: new HttpContext().set(SKIP_BUSY, true) };

const CART_API = `${API_URL}/cart`;
// Cada cuánto se revisa si el carrito cambió desde otro dispositivo
const REFRESH_MS = 10_000;

// Carrito de compras.
// - Invitado: solo localStorage.
// - Cliente: el carrito vive en el servidor (compartido con la app). localStorage queda
//   solo como copia para mostrar algo al instante mientras llega la respuesta.
@Injectable({ providedIn: 'root' })
export class CartService {
  private auth = inject(AuthService);
  private productService = inject(ProductService);
  private http = inject(HttpClient);

  // Llave de localStorage del carrito que se está usando ahora mismo
  private currentKey: string | null = null;
  // Id del cliente con sesión (null = invitado u otro rol: carrito solo local)
  private clientId: number | null = null;
  // Peticiones nuestras en vuelo: mientras haya, no se pisa el carrito con una lectura vieja
  private pending = 0;

  private itemsSubject = new BehaviorSubject<CartItem[]>([]);
  items$ = this.itemsSubject.asObservable();

  constructor() {
    this.auth.session$.subscribe((session) => this.switchCart(session));

    // Si la app cambió el carrito, aquí se refleja
    merge(interval(REFRESH_MS), fromEvent(document, 'visibilitychange'))
      .pipe(filter(() => document.visibilityState === 'visible'))
      .subscribe(() => this.refresh());
  }

  get items(): CartItem[] {
    return this.itemsSubject.value;
  }

  // =========================
  // Modificación del carrito
  // =========================

  // Si el mismo producto con los mismos adicionales ya está, se suma la cantidad
  addItem(product: Product, quantity: number, addOns: AddOnOption[]) {
    const id = this.buildId(product, addOns);
    const exists = this.items.some((item) => item.id === id);

    const updated = exists
      ? this.items.map((item) =>
          item.id === id
            ? { ...item, quantity: item.quantity + quantity }
            : item,
        )
      : [...this.items, { id, product, quantity, addOns }];

    this.setItems(updated);

    if (this.clientId !== null) {
      this.sync(
        this.http.post<CartItem[]>(`${CART_API}/me/items`, {
          productId: product.id,
          quantity,
          addOnIds: addOns.map((addOn) => addOn.id),
        }, BACKGROUND),
      );
    }
  }

  // Quita del carrito los productos que ya no están disponibles y devuelve sus nombres.
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

        unavailable.forEach((item) => this.removeItem(item.id));

        return [...new Set(unavailable.map((item) => item.product.name))];
      }),
      catchError(() => of([] as string[])),
    );
  }

  // Elimina una línea completa del carrito
  removeItem(id: string) {
    this.setItems(this.items.filter((item) => item.id !== id));

    if (this.clientId !== null) {
      this.sync(
        this.http.delete<CartItem[]>(
          `${CART_API}/me/items/${encodeURIComponent(id)}`,
          BACKGROUND,
        ),
      );
    }
  }

  // Vacía el carrito (por ejemplo, después de confirmar un pedido)
  clear() {
    this.setItems([]);

    if (this.clientId !== null) {
      this.sync(this.http.delete<CartItem[]>(`${CART_API}/me`, BACKGROUND));
    }
  }

  // Vuelve a leer el carrito del servidor (solo clientes)
  refresh(): void {
    const id = this.clientId;
    if (id === null || this.pending > 0) return;

    this.http
      .get<CartItem[]>(`${CART_API}/me`)
      .pipe(catchError(() => EMPTY))
      .subscribe((items) => {
        // Si mientras tanto cambió de dueño o hay cambios en vuelo, esta lectura ya no sirve
        if (this.clientId !== id || this.pending > 0) return;
        if (JSON.stringify(items) !== JSON.stringify(this.items)) {
          this.setItems(items);
        }
      });
  }

  // =========================
  // Cálculos
  // =========================

  unitPrice(item: CartItem): number {
    const addOnsTotal = item.addOns.reduce(
      (sum, addOn) => sum + addOn.price,
      0,
    );
    return item.product.price + addOnsTotal;
  }

  get total(): number {
    return this.items.reduce(
      (sum, item) => sum + this.unitPrice(item) * item.quantity,
      0,
    );
  }

  get count(): number {
    return this.items.reduce((sum, item) => sum + item.quantity, 0);
  }

  // =========================
  // Identificación de líneas (misma regla que el servidor)
  // =========================

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

  private keyFor(session: Session | null): string {
    return session && session.type === 'client'
      ? `${GUEST_KEY}_client_${session.id}`
      : GUEST_KEY;
  }

  private switchCart(session: Session | null) {
    const newKey = this.keyFor(session);

    // La sesión se vuelve a emitir al editar el perfil: mismo dueño, nada que cambiar
    if (newKey === this.currentKey) return;

    const previousKey = this.currentKey;
    this.currentKey = newKey;
    this.clientId = session && session.type === 'client' ? session.id : null;

    // Copia local: se muestra al instante mientras llega la del servidor
    const cached = this.load(newKey);
    let items = cached;
    // Lo que hay que subir al servidor: lo que traía el invitado y, la primera vez
    // que este cliente usa el carrito compartido, lo que ya tenía guardado en este navegador
    let toUpload: CartItem[] = [];

    if (previousKey === GUEST_KEY && newKey !== GUEST_KEY) {
      toUpload = this.items;
      items = this.merge(items, this.items);
      this.remove(GUEST_KEY);
    }

    if (this.clientId !== null && !this.wasSynced(this.clientId)) {
      toUpload = this.merge(toUpload, cached);
      this.markSynced(this.clientId);
    }

    this.setItems(items);

    if (this.clientId !== null) {
      if (toUpload.length > 0) {
        this.sync(
          this.http.post<CartItem[]>(
            `${CART_API}/me/items/bulk`,
            toUpload.map((item) => ({
              productId: item.product.id,
              quantity: item.quantity,
              addOnIds: item.addOns.map((addOn) => addOn.id),
            })),
            BACKGROUND,
          ),
        );
      } else {
        this.refresh();
      }
    }
  }

  private merge(base: CartItem[], extra: CartItem[]): CartItem[] {
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
  // Sincronización con el servidor
  // =========================

  // Envía un cambio. La respuesta del servidor es el carrito oficial; si falla, se vuelve a leer.
  private sync(request: Observable<CartItem[]>): void {
    this.pending++;
    request.subscribe({
      next: (items) => {
        this.pending--;
        if (this.pending === 0) this.setItems(items);
      },
      error: () => {
        this.pending--;
        this.refresh();
      },
    });
  }

  // Marca para subir el carrito guardado en el navegador solo una vez por cliente
  private wasSynced(clientId: number): boolean {
    try {
      return (
        localStorage.getItem(`terragalega_cart_synced_${clientId}`) === '1'
      );
    } catch {
      return true;
    }
  }

  private markSynced(clientId: number): void {
    try {
      localStorage.setItem(`terragalega_cart_synced_${clientId}`, '1');
    } catch {
      // Sin almacenamiento, el carrito del servidor sigue siendo el oficial
    }
  }

  // =========================
  // Persistencia local (copia)
  // =========================

  private setItems(items: CartItem[]) {
    this.itemsSubject.next(items);
    this.save(items);
  }

  private save(items: CartItem[]) {
    if (!this.currentKey) return;

    try {
      localStorage.setItem(this.currentKey, JSON.stringify(items));
    } catch {
      // Si el navegador bloquea el almacenamiento, el carrito sigue en memoria
    }
  }

  private remove(key: string) {
    try {
      localStorage.removeItem(key);
    } catch {
      // Sin almacenamiento no hay nada que borrar
    }
  }

  private load(key: string): CartItem[] {
    try {
      const raw = localStorage.getItem(key);
      if (!raw) return [];

      const parsed = JSON.parse(raw);
      if (!Array.isArray(parsed)) return [];

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
