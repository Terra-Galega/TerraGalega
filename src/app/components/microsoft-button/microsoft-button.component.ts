import { Component, EventEmitter, Input, OnInit, Output, signal } from '@angular/core';
import { PublicClientApplication } from '@azure/msal-browser';
import { MICROSOFT_CLIENT_ID } from '../../microsoft.config';
import { TranslatePipe } from '../../services/translate.pipe';

@Component({
  selector: 'app-microsoft-button',
  imports: [TranslatePipe],
  template: `
    @if (enabled) {
      <button type="button" class="w-full rounded-full border px-4 py-2 mb-3"
              [disabled]="disabled || !ready() || busy()" (click)="login()">
        {{ 'microsoft.continue' | t }}
      </button>
      @if (error()) { <p role="alert" class="mb-3 text-sm text-red-800">{{ error() }}</p> }
    }
  `,
})
export class MicrosoftButtonComponent implements OnInit {
  @Input() disabled = false;
  @Output() credential = new EventEmitter<string>();
  readonly enabled = MICROSOFT_CLIENT_ID.trim().length > 0;
  readonly ready = signal(false);
  readonly busy = signal(false);
  readonly error = signal('');
  private client?: PublicClientApplication;

  async ngOnInit() {
    if (!this.enabled) return;
    try {
      this.client = new PublicClientApplication({
        auth: { clientId: MICROSOFT_CLIENT_ID, authority: 'https://login.microsoftonline.com/common',
          redirectUri: location.origin + '/login' },
        cache: { cacheLocation: 'memoryStorage' },
      });
      await this.client.initialize();
      this.ready.set(true);
    } catch {
      this.error.set('No se pudo preparar el acceso con Microsoft.');
    }
  }

  async login() {
    if (!this.client || this.busy() || this.disabled) return;
    this.error.set('');
    this.busy.set(true);
    try {
      const account = await this.client.loginPopup({ scopes: ['openid', 'profile', 'email'], prompt: 'select_account' });
      this.credential.emit(account.idToken);
    } catch (error) {
      if (!(error instanceof Error && 'errorCode' in error && error.errorCode === 'user_cancelled')) {
        this.error.set('No se pudo abrir o completar el acceso con Microsoft.');
      }
    } finally {
      this.busy.set(false);
    }
  }
}
