import { Component, DestroyRef, ElementRef, HostListener, ViewChild, computed, inject, signal } from '@angular/core';
import { DatePipe } from '@angular/common';
import { HttpClient } from '@angular/common/http';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { EMPTY, catchError, distinctUntilChanged, exhaustMap, finalize, map, of, switchMap, timer } from 'rxjs';
import { API_URL } from '../../../../api-url';
import { AuthService } from '../../../../services/auth.service';
import { ToastService } from '../../../../services/toast.service';

interface ContactRequest { id: number; phone: string; requestedAt: string; read: boolean; handled: boolean; }

@Component({
  selector: 'app-operator-contact-requests',
  imports: [DatePipe],
  templateUrl: './operator-contact-requests.component.html',
  styleUrl: './operator-contact-requests.component.scss',
})
export class OperatorContactRequestsComponent {
  private readonly http = inject(HttpClient);
  private readonly toast = inject(ToastService);
  private readonly auth = inject(AuthService);
  private readonly destroy = inject(DestroyRef);
  private readonly url = `${API_URL}/operators/whatsapp-requests`;
  private readonly host = inject(ElementRef<HTMLElement>);
  @ViewChild('bell') private bell?: ElementRef<HTMLButtonElement>;
  readonly requests = signal<ContactRequest[]>([]);
  readonly error = signal(false);
  readonly busy = signal<Set<number>>(new Set());
  readonly expanded = signal(false);
  readonly unread = computed(() => this.requests().filter(item => !item.read).length);
  readonly readingAll = signal(false);

  constructor() {
    this.auth.session$.pipe(
      map(session => session?.type === 'operator' ? session.id : null),
      distinctUntilChanged(),
      switchMap(id => {
        this.expanded.set(false);
        this.requests.set([]);
        this.error.set(false);
        return id === null ? EMPTY : timer(0, 10000).pipe(
          exhaustMap(() => this.http.get<ContactRequest[]>(`${this.url}/notifications`).pipe(catchError(() => of(null)))),
        );
      }),
      takeUntilDestroyed(this.destroy),
    ).subscribe(list => {
      this.error.set(list === null);
      if (list === null) return;
      this.requests.set(list);
    });
  }

  handled(item: ContactRequest): void {
    if (this.busy().has(item.id)) return;
    this.busy.update(value => new Set(value).add(item.id));
    this.http.put<void>(`${this.url}/${encodeURIComponent(item.phone)}/handled`, {}, {
      params: { requestedAt: item.requestedAt },
    }).pipe(
      finalize(() => this.busy.update(value => { const next = new Set(value); next.delete(item.id); return next; })),
      takeUntilDestroyed(this.destroy),
    ).subscribe({
      next: () => this.requests.update(list => list.map(current => current.id === item.id ? { ...current, read: true, handled: true } : current)),
      error: () => this.toast.error('No se pudo actualizar la solicitud', 'Vuelve a intentarlo; puede haberse recibido una solicitud nueva.'),
    });
  }


  @HostListener('document:click', ['$event'])
  closeOutside(event: MouseEvent): void {
    if (!this.host.nativeElement.contains(event.target as Node)) this.expanded.set(false);
  }

  @HostListener('document:keydown.escape')
  closeWithEscape(): void {
    if (this.expanded()) {
      this.expanded.set(false);
      this.bell?.nativeElement.focus();
    }
  }

  markRead(item: ContactRequest): void {
    if (item.read) return;
    this.http.put<void>(`${this.url}/notifications/${item.id}/read`, {}).pipe(takeUntilDestroyed(this.destroy)).subscribe({
      next: () => this.requests.update(list => list.map(current => current.id === item.id ? { ...current, read: true } : current)),
      error: () => this.toast.error('No se pudo marcar la notificación como leída'),
    });
  }

  markAllRead(): void {
    if (this.readingAll()) return;
    const ids = new Set(this.requests().map(item => item.id));
    this.readingAll.set(true);
    this.http.put<void>(`${this.url}/notifications/read-all`, {}).pipe(
      finalize(() => this.readingAll.set(false)), takeUntilDestroyed(this.destroy),
    ).subscribe({
      next: () => this.requests.update(list => list.map(item => ids.has(item.id) ? { ...item, read: true } : item)),
      error: () => this.toast.error('No se pudieron marcar las notificaciones como leídas'),
    });
  }

  async copy(phone: string): Promise<void> {
    try {
      await navigator.clipboard.writeText(`+${phone}`);
      this.toast.show('Número copiado');
    } catch {
      this.toast.error('No se pudo copiar', 'Selecciona y copia el número que aparece en la solicitud.');
    }
  }
}
