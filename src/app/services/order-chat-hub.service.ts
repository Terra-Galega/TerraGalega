import { Injectable, computed, inject, signal } from '@angular/core';
import { Subject, Subscription, catchError, filter, merge, of, switchMap, timer } from 'rxjs';
import { OrderChatService } from './order-chat.service';
import { ChatRole, OrderChatSummary } from '../models/order-chat';

const POLL_MS = 6000;

// Estado compartido del chat con el operador:
// - qué pedidos tienen chat activo (para mostrar la opción "Operador" y los puntitos),
// - qué chat está abierto en el panel flotante,
// - qué mensajes ya se vieron (localStorage).
@Injectable({ providedIn: 'root' })
export class OrderChatHubService {
  private api = inject(OrderChatService);

  readonly summaries = signal<OrderChatSummary[]>([]);
  readonly openOrderId = signal<number | null>(null);

  private readonly role = signal<ChatRole | null>(null);
  private readonly seen = signal<Record<number, number>>({});

  // Avisa cuando se abre un chat (el widget cierra a Galleguito)
  private readonly openedSubject = new Subject<number>();
  readonly opened$ = this.openedSubject.asObservable();

  // Botón que abrió el chat: recibe el foco al cerrarlo
  opener: HTMLElement | null = null;

  private storageKey = '';
  private poll?: Subscription;
  private refreshSubject = new Subject<void>();

  // Pedidos con el puntito:
  // - Cliente: mensajes del operador que todavía no ha visto.
  // - Operador: el último mensaje es del cliente, o sea que está esperando respuesta.
  //   El punto sigue aunque abra y cierre el chat; se quita cuando responde.
  // En los dos casos no cuenta el chat que está abierto en ese momento.
  readonly unreadIds = computed(() => {
    const role = this.role();
    if (!role) return new Set<number>();
    const other: ChatRole = role === 'CLIENT' ? 'OPERATOR' : 'CLIENT';
    const seen = this.seen();
    const openId = this.openOrderId();
    return new Set(
      this.summaries()
        .filter(
          (s) =>
            s.lastSenderRole === other &&
            s.orderId !== openId &&
            (role === 'OPERATOR' || s.lastMessageId > (seen[s.orderId] ?? 0)),
        )
        .map((s) => s.orderId),
    );
  });

  readonly hasUnread = computed(() => this.unreadIds().size > 0);

  isUnread(orderId: number): boolean {
    return this.unreadIds().has(orderId);
  }

  // Empieza a consultar los chats activos del usuario con sesión
  start(role: ChatRole, userId: number) {
    const key = `terragalega_order_chat_seen_${role}_${userId}`;
    if (this.poll && this.storageKey === key) return;

    this.stop();
    this.role.set(role);
    this.storageKey = key;
    this.seen.set(this.loadSeen());

    this.poll = merge(timer(0, POLL_MS), this.refreshSubject)
      .pipe(
        filter(() => document.visibilityState === 'visible'),
        switchMap(() => this.api.getActive().pipe(catchError(() => of(null)))),
      )
      .subscribe((list) => {
        if (list) this.summaries.set(list);
      });
  }

  stop() {
    this.poll?.unsubscribe();
    this.poll = undefined;
    this.summaries.set([]);
    this.openOrderId.set(null);
    this.role.set(null);
    this.opener = null;
  }

  refresh() {
    this.refreshSubject.next();
  }

  open(orderId: number, opener: HTMLElement | null = null) {
    this.opener = opener;
    this.openOrderId.set(orderId);
    this.openedSubject.next(orderId);
  }

  // Cambiar de pedido sin cerrar el panel
  switchTo(orderId: number) {
    this.openOrderId.set(orderId);
  }

  close() {
    this.openOrderId.set(null);
    this.refresh();
  }

  markSeen(orderId: number, lastId: number) {
    const current = this.seen();
    if ((current[orderId] ?? 0) >= lastId) return;
    const next = { ...current, [orderId]: lastId };
    this.seen.set(next);
    try {
      localStorage.setItem(this.storageKey, JSON.stringify(next));
    } catch {
      // Sin localStorage: lo leído solo vive en memoria
    }
  }

  private loadSeen(): Record<number, number> {
    try {
      return JSON.parse(localStorage.getItem(this.storageKey) ?? '{}');
    } catch {
      return {};
    }
  }
}