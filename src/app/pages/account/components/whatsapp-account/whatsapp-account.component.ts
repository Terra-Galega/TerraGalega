import { Component, DestroyRef, inject, signal } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { finalize } from 'rxjs';
import { WhatsAppAccountService, WhatsAppAccountStatus } from '../../../../services/whatsapp-account.service';
import { TranslatePipe } from '../../../../pipes/translate.pipe';
import { getErrorMessage } from '../../../../utils/http-error';

@Component({
  selector: 'app-whatsapp-account',
  imports: [TranslatePipe],
  templateUrl: './whatsapp-account.component.html',
})
export class WhatsAppAccountComponent {
  private accounts = inject(WhatsAppAccountService);
  private destroyRef = inject(DestroyRef);
  status = signal<WhatsAppAccountStatus | null>(null);
  busy = signal(false);
  confirming = signal(false);
  error = signal('');
  constructor() { this.load(); }

  load(): void {
    if (this.busy()) return;
    this.busy.set(true);
    this.error.set('');
    this.accounts.status().pipe(takeUntilDestroyed(this.destroyRef), finalize(() => this.busy.set(false))).subscribe({
      next: status => this.status.set(status), error: error => this.error.set(getErrorMessage(error)),
    });
  }

  unlink(): void {
    if (!this.confirming() || this.busy()) return;
    this.busy.set(true);
    this.error.set('');
    this.accounts.unlink().pipe(takeUntilDestroyed(this.destroyRef), finalize(() => this.busy.set(false))).subscribe({
      next: () => {
        this.accounts.clearPending();
        this.status.set({ linked: false, phone: null, linkedAt: null });
        this.confirming.set(false);
      },
      error: error => this.error.set(getErrorMessage(error)),
    });
  }
}
