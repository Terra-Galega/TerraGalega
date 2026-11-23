import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { API_URL } from '../api-url';

export interface WhatsAppAccountStatus { linked: boolean; phone: string | null; linkedAt: string | null; }
export interface WhatsAppLinkPreview { maskedPhone: string; expiresAt: string; }

@Injectable({ providedIn: 'root' })
export class WhatsAppAccountService {
  private http = inject(HttpClient);
  private url = `${API_URL}/clients/me/whatsapp`;
  private key = 'terragalega_whatsapp_link';
  private pending: { token: string; until: number } | null = null;

  status() { return this.http.get<WhatsAppAccountStatus>(this.url); }
  preview(token: string) { return this.http.post<WhatsAppLinkPreview>(`${this.url}/preview`, { token }); }
  confirm(token: string) { return this.http.post<WhatsAppAccountStatus>(`${this.url}/confirm`, { token }); }
  unlink() { return this.http.delete<void>(this.url); }

  // Se conserva solo en esta pestaña para volver del login; jamás en la URL del login.
  remember(token: string): void {
    this.clearPending();
    if (!/^[A-Za-z0-9_-]{43}$/.test(token)) return;
    this.pending = { token, until: Date.now() + 15 * 60_000 };
    try { sessionStorage.setItem(this.key, JSON.stringify(this.pending)); } catch { }
  }

  pendingToken(): string {
    try {
      const pending = this.pending ?? JSON.parse(sessionStorage.getItem(this.key) ?? 'null');
      if (pending && pending.until > Date.now() && /^[A-Za-z0-9_-]{43}$/.test(pending.token)) return pending.token;
    } catch { }
    this.clearPending();
    return '';
  }

  clearPending(): void {
    this.pending = null;
    try { sessionStorage.removeItem(this.key); } catch { }
  }
}
