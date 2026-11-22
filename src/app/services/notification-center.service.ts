import { HttpClient } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import {
  BehaviorSubject,
  EMPTY,
  catchError,
  exhaustMap,
  map,
  of,
  switchMap,
  timer,
} from 'rxjs';
import { API_URL } from '../api-url';
import { AuthService } from './auth.service';
import { OrderChatHubService } from './order-chat-hub.service';
import { ToastService } from './toast.service';

// Lo que manda el back en GET /notifications/me
interface NotificationDTO {
  id: number;
  type: string;
  title: string;
  message: string;
  orderId: number | null;
  read: boolean;
  createdAt: string; // ISO
}

export interface AppNotification {
  id: number;
  type: string;
  title: string;
  message: string;
  createdAt: string; // ISO
  read: boolean;
  orderId: number | null;
  link: string; // ruta a la que lleva, p. ej. '/orders/tracking'
  queryParams?: Record<string, string | number>;
}

// Cada cuánto se consultan las notificaciones al back (milisegundos)
const POLL_MS = 10_000;

// Las guarda el back (así existen aunque la web esté cerrada). Aquí solo se
// leen cada pocos segundos y se muestran en la campanita.
@Injectable({ providedIn: 'root' })
export class NotificationCenterService {
  private auth = inject(AuthService);
  private http = inject(HttpClient);
  private toast = inject(ToastService);
  private chatHub = inject(OrderChatHubService);

  private subject = new BehaviorSubject<AppNotification[]>([]);
  // Ids ya conocidos, para avisar en pantalla solo de las nuevas
  private knownIds = new Set<number>();
  private firstLoad = true;

  readonly notifications$ = this.subject.asObservable();
  readonly unreadCount$ = this.subject.pipe(
    map((list) => list.filter((n) => !n.read).length),
  );

  constructor() {
    this.auth.session$
      .pipe(
        switchMap((session) => {
          this.knownIds.clear();
          this.firstLoad = true;
          this.subject.next([]);

          if (!session || session.type !== 'client') {
            return EMPTY;
          }

          return timer(0, POLL_MS).pipe(
            exhaustMap(() =>
              this.http
                .get<NotificationDTO[]>(`${API_URL}/notifications/me`, {
                  params: { platform: 'web' },
                })
                .pipe(catchError(() => of(null))),
            ),
          );
        }),
      )
      .subscribe((list) => {
        if (list) this.receive(list);
      });
  }

  markRead(id: number): void {
    this.subject.next(
      this.subject.value.map((n) => (n.id === id ? { ...n, read: true } : n)),
    );
    this.http
      .put(`${API_URL}/notifications/${id}/read`, {})
      .subscribe({ error: () => {} });
  }

  markAllRead(): void {
    this.subject.next(this.subject.value.map((n) => ({ ...n, read: true })));
    this.http
      .put(`${API_URL}/notifications/me/read-all`, {})
      .subscribe({ error: () => {} });
  }

  clear(): void {
    this.subject.next([]);
    this.http
      .delete(`${API_URL}/notifications/me`)
      .subscribe({ error: () => {} });
  }

  private receive(list: NotificationDTO[]): void {
    const items = list.map((n) => this.toItem(n));
    const openChatId = this.chatHub.openOrderId();

    // Mensaje del operador de un chat que el cliente ya tiene abierto: ya lo está viendo,
    // así que se marca leído y no se avisa en pantalla
    for (const n of items) {
      if (
        n.type === 'ORDER_CHAT_MESSAGE' &&
        !n.read &&
        n.orderId === openChatId
      ) {
        n.read = true;
        this.http
          .put(`${API_URL}/notifications/${n.id}/read`, {})
          .subscribe({ error: () => {} });
      }
    }

    // Aviso en pantalla de las nuevas (no en la primera carga). "Salió" y
    // "ya va llegando" los avisa ClientDeliveryWatcherService (con vibración).
    if (!this.firstLoad) {
      for (const n of items) {
        const handledByWatcher =
          n.type === 'ORDER_ON_THE_WAY' || n.type === 'ORDER_NEAR';
        if (!n.read && !this.knownIds.has(n.id) && !handledByWatcher) {
          this.toast.show(n.title, n.message, 6000);
        }
      }
    }

    items.forEach((n) => this.knownIds.add(n.id));
    this.firstLoad = false;
    this.subject.next(items);
  }

  // A dónde lleva cada tipo al tocarla
  private toItem(n: NotificationDTO): AppNotification {
    let link = '/home';
    let queryParams: Record<string, string | number> | undefined;

    if (
      n.type === 'ORDER_CONFIRMED' ||
      n.type === 'ORDER_PREPARING' ||
      n.type === 'ORDER_CHAT_MESSAGE'
    ) {
      link = '/orders';
    } else if (n.type === 'ORDER_ON_THE_WAY' || n.type === 'ORDER_NEAR') {
      link = '/orders/tracking';
      queryParams = { order: n.orderId as number };
    } else if (n.type === 'COUPON') {
      link = '/coupons';
    } else if (n.type === 'WELCOME') {
      link = '/menu';
    }

    return { ...n, link, queryParams };
  }
}
