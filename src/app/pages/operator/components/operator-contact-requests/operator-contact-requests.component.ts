import { Component, DestroyRef, inject, signal } from '@angular/core';
import { DatePipe } from '@angular/common';
import { HttpClient } from '@angular/common/http';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { EMPTY, catchError, distinctUntilChanged, exhaustMap, finalize, map, of, switchMap, timer } from 'rxjs';
import { API_URL } from '../../../../api-url';
import { AuthService } from '../../../../services/auth.service';
import { ToastService } from '../../../../services/toast.service';

interface ContactRequest { phone: string; requestedAt: string; }

@Component({
  selector: 'app-operator-contact-requests',
  imports: [DatePipe],
  templateUrl: './operator-contact-requests.component.html',
})
export class OperatorContactRequestsComponent {
  private readonly http = inject(HttpClient);
  private readonly toast = inject(ToastService);
  private readonly auth = inject(AuthService);
  private readonly destroy = inject(DestroyRef);
  private readonly url = `${API_URL}/operators/whatsapp-requests`;
  private known = new Set<string>();
  readonly requests = signal<ContactRequest[]>([]);
  readonly error = signal(false);
  readonly busy = signal<Set<string>>(new Set());
  readonly expanded = signal(true);

  constructor() {
    this.auth.session$.pipe(
      map(session => session?.type === 'operator' ? session.id : null),
      distinctUntilChanged(),
      switchMap(id => {
        this.known.clear();
        this.requests.set([]);
        this.error.set(false);
        return id === null ? EMPTY : timer(0, 10000).pipe(
          exhaustMap(() => this.http.get<ContactRequest[]>(this.url).pipe(catchError(() => of(null)))),
        );
      }),
      takeUntilDestroyed(this.destroy),
    ).subscribe(list => {
      this.error.set(list === null);
      if (list === null) return;
      const fresh = list.filter(item => !this.known.has(`${item.phone}:${item.requestedAt}`));
      for (const item of list) this.known.add(`${item.phone}:${item.requestedAt}`);
      if (fresh.length) {
        this.expanded.set(true);
        this.toast.show('Solicitud de atención', fresh.length === 1
          ? `Un cliente solicita atención. Comunícate al +${fresh[0].phone}.`
          : `${fresh.length} clientes solicitan que un operador se comunique con ellos.`, 6000);
      }
      this.requests.set(list);
    });
  }

  handled(item: ContactRequest): void {
    if (this.busy().has(item.phone)) return;
    this.busy.update(value => new Set(value).add(item.phone));
    this.http.put<void>(`${this.url}/${encodeURIComponent(item.phone)}/handled`, {}, {
      params: { requestedAt: item.requestedAt },
    }).pipe(
      finalize(() => this.busy.update(value => { const next = new Set(value); next.delete(item.phone); return next; })),
      takeUntilDestroyed(this.destroy),
    ).subscribe({
      next: () => this.requests.update(list => list.filter(current => current.phone !== item.phone || current.requestedAt !== item.requestedAt)),
      error: () => this.toast.error('No se pudo actualizar la solicitud', 'Vuelve a intentarlo; puede haberse recibido una solicitud nueva.'),
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
