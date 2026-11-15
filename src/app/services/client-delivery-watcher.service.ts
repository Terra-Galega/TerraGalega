import { Injectable, inject } from '@angular/core';
import {
  BehaviorSubject,
  EMPTY,
  catchError,
  exhaustMap,
  of,
  switchMap,
  timer,
} from 'rxjs';

import { AuthService } from './auth.service';
import { DeliveryTrackingService } from './delivery-tracking.service';
import { ToastService } from './toast.service';
import { ClientDelivery } from '../models/delivery-tracking';
import { Router } from '@angular/router';

// Cada cuánto se consulta al back el pedido en camino (milisegundos)
const POLL_MS = 4_000;
// Cuando el recorrido llega a este avance se avisa "ya va llegando" (85 %)
export const NEAR_PROGRESS = 0.85;

// Vibración: una pulsación corta al salir; un patrón largo cuando ya va llegando
const VIBRATE_STARTED = [250];
const VIBRATE_NEAR = [500, 200, 500, 200, 500];

const STORAGE_KEY = 'terragalega_delivery_alerts';

// Qué avisos ya se mostraron de cada recorrido (clave: pedido + hora de inicio)
interface AlertState {
  started?: boolean;
  near?: boolean;
}

// Vigila en segundo plano (desde cualquier página) el pedido en camino del
// cliente. Hace dos cosas:
//  1. Comparte la última respuesta (`deliveries$`) con la pantalla de seguimiento,
//     para no consultar al back dos veces.
//  2. Avisa (aviso en pantalla + notificación del navegador + vibración) cuando
//     el repartidor inicia el recorrido y cuando va por el 85 %.
// Cada aviso se muestra una sola vez por recorrido, aunque se recargue la página.
@Injectable({ providedIn: 'root' })
export class ClientDeliveryWatcherService {
  private auth = inject(AuthService);
  private tracking = inject(DeliveryTrackingService);
  private toast = inject(ToastService);

  private router = inject(Router);

  private started = false;
  private subject = new BehaviorSubject<ClientDelivery[]>([]);
  // true después de la primera respuesta del back (para distinguir "cargando" de "vacío")
  private loadedSubject = new BehaviorSubject<boolean>(false);

  readonly deliveries$ = this.subject.asObservable();
  readonly loaded$ = this.loadedSubject.asObservable();

  // Se llama una vez al arrancar la app
  start(): void {
    if (this.started) return;
    this.started = true;

    this.auth.session$
      .pipe(
        switchMap((session) => {
          if (!session || session.type !== 'client') {
            this.subject.next([]);
            this.loadedSubject.next(false);
            return EMPTY;
          }

          return timer(0, POLL_MS).pipe(
            // Evita acumular peticiones si una respuesta tarda
            exhaustMap(() =>
              this.tracking.getClientActive(session.id).pipe(
                // Si falla (sin conexión, etc.) se conserva lo último y se reintenta
                catchError(() => of(null)),
              ),
            ),
          );
        }),
      )
      .subscribe((deliveries) => {
        if (!deliveries) return;
        this.subject.next(deliveries);
        this.loadedSubject.next(true);
        this.checkAlerts(deliveries);
      });
  }

  // Consulta inmediata (por ejemplo, al abrir la pantalla de seguimiento)
  refresh(): void {
    const session = this.auth.session;
    if (!session || session.type !== 'client') return;

    this.tracking
      .getClientActive(session.id)
      .pipe(catchError(() => of(null)))
      .subscribe((deliveries) => {
        if (!deliveries) return;
        this.subject.next(deliveries);
        this.loadedSubject.next(true);
        this.checkAlerts(deliveries);
      });
  }

  // =========================
  // Permiso de notificaciones del navegador
  // =========================

  get notificationsSupported(): boolean {
    return typeof window !== 'undefined' && 'Notification' in window;
  }

  // 'granted' | 'denied' | 'default' | 'unsupported'
  get notificationPermission(): NotificationPermission | 'unsupported' {
    return this.notificationsSupported
      ? Notification.permission
      : 'unsupported';
  }

  // Debe llamarse desde un clic del usuario (los navegadores lo exigen)
  async enableNotifications(): Promise<void> {
    if (!this.notificationsSupported || Notification.permission !== 'default') {
      return;
    }
    try {
      await Notification.requestPermission();
    } catch {
      // Navegadores antiguos: no hay nada que hacer
    }
  }

  // =========================
  // Avisos
  // =========================

  private checkAlerts(deliveries: ClientDelivery[]): void {
    const state = this.loadState();
    const activeKeys = new Set<string>();
    let changed = false;

    for (const delivery of deliveries) {
      // Mientras el repartidor no inicie el recorrido no hay nada que avisar
      if (!delivery.startedAt) continue;

      const key = `${delivery.orderId}:${delivery.startedAt}`;
      activeKeys.add(key);
      const entry = state[key] ?? (state[key] = {});

      const nearDue =
        !entry.near &&
        delivery.progress !== null &&
        delivery.progress >= NEAR_PROGRESS;

      if (nearDue) {
        // Si se llega aquí sin haber visto el inicio, solo importa el aviso de llegada
        entry.near = true;
        entry.started = true;
        changed = true;
        this.alert(
          '¡Ya va llegando! Alístate',
          `Pedido #${delivery.orderId}: tu repartidor está a punto de llegar. Ten tu PIN a la mano.`,
          `near-${key}`,
          VIBRATE_NEAR,
          delivery.orderId,
        );
      } else if (!entry.started) {
        entry.started = true;
        changed = true;
        this.alert(
          '¡Tu pedido ya va en camino!',
          `Pedido #${delivery.orderId}: ${
            delivery.deliveryPersonName ?? 'el repartidor'
          } inició el recorrido. Puedes seguirlo en el mapa.`,
          `started-${key}`,
          VIBRATE_STARTED,
          delivery.orderId,
        );
      }
    }

    // Limpia los recorridos que ya terminaron
    for (const key of Object.keys(state)) {
      if (!activeKeys.has(key)) {
        delete state[key];
        changed = true;
      }
    }

    if (changed) this.saveState(state);
  }

  private alert(
    title: string,
    message: string,
    tag: string,
    vibration: number[],
    orderId: number,
  ): void {
    // 0) Bandeja (campanita)

    // 1) Aviso dentro de la página
    this.toast.show(title, message, 9000);

    // 2) Vibración (solo celulares/tablets con navegador compatible)
    try {
      if ('vibrate' in navigator) navigator.vibrate(vibration);
    } catch {
      // El navegador puede bloquearla si el usuario aún no ha tocado la página
    }

    // 3) Notificación del sistema (se ve aunque la pestaña esté en segundo plano)
    try {
      if (
        this.notificationsSupported &&
        Notification.permission === 'granted'
      ) {
        const notification = new Notification(title, {
          body: message,
          tag,
          icon: '/favicon.ico',
        });
        notification.onclick = () => {
          window.focus();
          notification.close();
          void this.router.navigate(['/orders/tracking'], {
            // <-- nuevo
            queryParams: { order: orderId },
          });
        };
      }
    } catch {
      // Chrome en Android no permite `new Notification()`: queda el aviso en pantalla
    }
  }

  private loadState(): Record<string, AlertState> {
    try {
      return JSON.parse(localStorage.getItem(STORAGE_KEY) ?? '{}');
    } catch {
      return {};
    }
  }

  private saveState(state: Record<string, AlertState>): void {
    try {
      localStorage.setItem(STORAGE_KEY, JSON.stringify(state));
    } catch {
      // Sin almacenamiento: el aviso podría repetirse tras recargar, no es grave
    }
  }
}
