import { Injectable, inject } from '@angular/core';
import { BehaviorSubject, map } from 'rxjs';
import { AuthService } from './auth.service';

export interface AppNotification {
  id: string;
  title: string;
  message: string;
  createdAt: string; // ISO
  read: boolean;
  link: string; // ruta a la que lleva, p. ej. '/orders/tracking'
  queryParams?: Record<string, string | number>;
}

const MAX_ITEMS = 30;

@Injectable({ providedIn: 'root' })
export class NotificationCenterService {
  private auth = inject(AuthService);

  private subject = new BehaviorSubject<AppNotification[]>([]);
  private userId: number | null = null;

  readonly notifications$ = this.subject.asObservable();
  readonly unreadCount$ = this.subject.pipe(
    map((list) => list.filter((n) => !n.read).length),
  );

  constructor() {
    this.auth.session$.subscribe((session) => {
      this.userId = session?.type === 'client' ? session.id : null;
      this.subject.next(this.userId === null ? [] : this.load());
    });
  }

  add(n: Omit<AppNotification, 'id' | 'createdAt' | 'read'>): void {
    if (this.userId === null) return;
    const item: AppNotification = {
      ...n,
      id: `${Date.now()}-${Math.random().toString(36).slice(2, 7)}`,
      createdAt: new Date().toISOString(),
      read: false,
    };
    this.update([item, ...this.subject.value].slice(0, MAX_ITEMS));
  }

  markRead(id: string): void {
    this.update(
      this.subject.value.map((n) => (n.id === id ? { ...n, read: true } : n)),
    );
  }

  markAllRead(): void {
    this.update(this.subject.value.map((n) => ({ ...n, read: true })));
  }

  clear(): void {
    this.update([]);
  }

  private update(list: AppNotification[]): void {
    this.subject.next(list);
    try {
      localStorage.setItem(this.key(), JSON.stringify(list));
    } catch {}
  }

  private load(): AppNotification[] {
    try {
      return JSON.parse(localStorage.getItem(this.key()) ?? '[]');
    } catch {
      return [];
    }
  }

  private key(): string {
    return `terragalega_notifications_${this.userId}`;
  }
}
