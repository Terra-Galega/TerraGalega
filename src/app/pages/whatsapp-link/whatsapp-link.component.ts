import { Component, DestroyRef, inject, signal } from '@angular/core';
import { DatePipe } from '@angular/common';
import { ActivatedRoute, RouterLink } from '@angular/router';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { finalize } from 'rxjs';
import { AuthService } from '../../services/auth.service';
import { WhatsAppAccountService, WhatsAppLinkPreview } from '../../services/whatsapp-account.service';
import { NavbarComponent } from '../../components/navbar/navbar.component';
import { FooterComponent } from '../../components/footer/footer.component';
import { TranslatePipe } from '../../pipes/translate.pipe';
import { getErrorMessage } from '../../utils/http-error';

@Component({
  selector: 'app-whatsapp-link',
  imports: [NavbarComponent, FooterComponent, RouterLink, DatePipe, TranslatePipe],
  templateUrl: './whatsapp-link.component.html',
})
export class WhatsAppLinkComponent {
  readonly auth = inject(AuthService);
  private accounts = inject(WhatsAppAccountService);
  private route = inject(ActivatedRoute);
  private destroyRef = inject(DestroyRef);
  preview = signal<WhatsAppLinkPreview | null>(null);
  loading = signal(false);
  saving = signal(false);
  done = signal(false);
  error = signal('');
  token = '';

  constructor() {
    const fragment = this.route.snapshot.fragment;
    if (fragment !== null) {
      this.accounts.remember(new URLSearchParams(fragment).get('token') ?? '');
      // El fragmento no viaja al servidor; también se quita del historial visible.
      window.history.replaceState(window.history.state, '', window.location.pathname + window.location.search);
    }
    this.token = this.accounts.pendingToken();
    if (this.auth.isLoggedIn && this.auth.session?.type === 'client' && !this.auth.needsProfile && this.token) this.load();
  }

  load(): void {
    if (this.loading() || this.saving()) return;
    this.loading.set(true);
    this.error.set('');
    this.accounts.preview(this.token).pipe(takeUntilDestroyed(this.destroyRef), finalize(() => this.loading.set(false))).subscribe({
      next: preview => this.preview.set(preview),
      error: error => this.error.set(getErrorMessage(error)),
    });
  }

  confirm(): void {
    if (!this.preview() || this.saving() || this.done()) return;
    this.saving.set(true);
    this.error.set('');
    this.accounts.confirm(this.token).pipe(takeUntilDestroyed(this.destroyRef), finalize(() => this.saving.set(false))).subscribe({
      next: () => { this.accounts.clearPending(); this.token = ''; this.done.set(true); },
      error: error => this.error.set(getErrorMessage(error)),
    });
  }

  cancel(): void { this.accounts.clearPending(); this.token = ''; this.preview.set(null); }
}
