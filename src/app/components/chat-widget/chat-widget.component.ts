import { Component, ElementRef, effect, inject, signal, viewChild } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { RouterLink } from '@angular/router';
import { AuthService } from '../../services/auth.service';
import { CartService } from '../../services/cart.service';
import { ChatService } from '../../services/chat.service';
import { ToastService } from '../../services/toast.service';
import { ChatBubble, ChatMessage } from '../../models/chat';
import { LanguageService } from '../../services/language.service';
import { TranslatePipe } from '../../pipes/translate.pipe';

// Máximo de mensajes que se guardan en sessionStorage
const MAX_STORED = 40;

// Asistente virtual flotante. Solo se muestra a clientes con sesión iniciada.
@Component({
  selector: 'app-chat-widget',
  imports: [RouterLink, TranslatePipe],
  templateUrl: './chat-widget.component.html',
  styleUrl: './chat-widget.component.scss',
})
export class ChatWidgetComponent {
  private auth = inject(AuthService);
  private cart = inject(CartService);
  private chat = inject(ChatService);
  private toast = inject(ToastService);
  readonly language = inject(LanguageService);

  readonly maxLength = 500;
  readonly suggestions = [
    'Recomiéndame un plato',
    'Tengo antojo de algo vegetariano',
    'Quiero hacer una reserva',
    '¿Qué tengo en el carrito?',
  ];

  readonly isClient = signal(false);
  readonly firstName = signal('');
  readonly open = signal(false);
  readonly sending = signal(false);
  readonly draft = signal('');
  readonly bubbles = signal<ChatBubble[]>([]);

  private clientId: number | null = null;
  private messagesEl = viewChild<ElementRef<HTMLElement>>('messages');
  private inputEl = viewChild<ElementRef<HTMLTextAreaElement>>('input');

  constructor() {
    // Cambia de cliente (login/logout): se carga o se limpia su conversación
    this.auth.session$.pipe(takeUntilDestroyed()).subscribe((session) => {
      const id = session?.type === 'client' ? session.id : null;
      if (id !== this.clientId) {
        if (this.clientId !== null && id === null) {
          this.removeStored(this.clientId);
        }
        this.clientId = id;
        this.open.set(false);
        this.draft.set('');
        this.bubbles.set(id !== null ? this.loadStored(id) : []);
      }
      this.isClient.set(id !== null);
      this.firstName.set(session?.name ?? '');
    });

    // Guarda la conversación para que sobreviva al ir a Stripe y volver
    effect(() => {
      const list = this.bubbles();
      if (this.clientId !== null) {
        try {
          sessionStorage.setItem(this.storageKey(this.clientId), JSON.stringify(list.slice(-MAX_STORED)));
        } catch {
          // Sin sessionStorage (modo privado): la conversación solo vive en memoria
        }
      }
    });

    // Baja al último mensaje cuando llega algo nuevo o se abre el panel
    effect(() => {
      this.bubbles();
      this.sending();
      if (this.open()) {
        setTimeout(() => {
          const el = this.messagesEl()?.nativeElement;
          if (el) {
            el.scrollTop = el.scrollHeight;
          }
        });
      }
    });
  }

  toggle(): void {
    this.open.update((v) => !v);
    if (this.open()) {
      setTimeout(() => this.inputEl()?.nativeElement.focus());
    }
  }

  reset(): void {
    if (!this.sending()) {
      this.bubbles.set([]);
      this.inputEl()?.nativeElement.focus();
    }
  }

  // Enter envía; Shift + Enter hace salto de línea
  onKeydown(event: KeyboardEvent): void {
    if (event.key === 'Enter' && !event.shiftKey) {
      event.preventDefault();
      this.send();
    }
  }

  send(text?: string): void {
    const message = (text ?? this.draft()).trim().slice(0, this.maxLength);
    if (!message || this.sending()) {
      return;
    }

    // Historial para el back: solo los mensajes que sí se enviaron y respondieron bien
    const history: ChatMessage[] = this.bubbles()
      .filter((b) => !b.error)
      .map((b) => ({ role: b.role, text: b.text }));

    const userBubble: ChatBubble = { role: 'user', text: message };
    this.bubbles.update((list) => [...list, userBubble]);
    this.draft.set('');
    this.sending.set(true);

    this.chat.send(message, history).subscribe({
      next: (res) => {
        this.bubbles.update((list) => [
          ...list,
          { role: 'model', text: res.reply, reservation: res.reservation },
        ]);
        if (res.cartChanged) {
          this.cart.refresh();
          this.toast.show('Carrito actualizado', 'El asistente agregó productos a tu carrito.');
        }
        this.sending.set(false);
      },
      error: (err) => {
        const msg = err?.error?.message || this.language.t('chat.connection_error');
        // El mensaje del cliente queda marcado para no mandarlo en el historial
        userBubble.error = true;
        this.bubbles.update((list) => [...list, { role: 'model', text: msg, error: true }]);
        this.sending.set(false);
      },
    });
  }

  // Lleva a Stripe; al pagar, Stripe devuelve a /reservations/result
  pay(url: string | null): void {
    if (url) {
      window.location.href = url;
    }
  }

  // Texto del bot -> HTML seguro: escapa todo y solo convierte **negrita**, viñetas y saltos de línea
  format(text: string): string {
    const escaped = text.replace(/&/g, '&amp;').replace(/</g, '&lt;').replace(/>/g, '&gt;');
    return escaped
      .replace(/\*\*(.+?)\*\*/g, '<strong>$1</strong>')
      .replace(/^\s*[-*]\s+/gm, '• ')
      .replace(/\n/g, '<br>');
  }

  formatDate(date: string): string {
    const locale = { es: 'es-CO', en: 'en-US', ca: 'ca-ES' }[this.language.language()];
    return new Date(date + 'T00:00').toLocaleDateString(locale, {
      weekday: 'long',
      day: 'numeric',
      month: 'long',
    });
  }

  money(value: number): string {
    const locale = { es: 'es-CO', en: 'en-US', ca: 'ca-ES' }[this.language.language()];
    return new Intl.NumberFormat(locale, {
      style: 'currency',
      currency: 'COP',
      maximumFractionDigits: 0,
    }).format(value);
  }

  // --- sessionStorage ---

  private storageKey(id: number): string {
    return `terragalega_chat_${id}`;
  }

  // Al recargar se quitan las tarjetas de reserva: su estado de pago pudo cambiar
  private loadStored(id: number): ChatBubble[] {
    try {
      const raw = sessionStorage.getItem(this.storageKey(id));
      const list: ChatBubble[] = raw ? JSON.parse(raw) : [];
      return list.map((b) => ({ role: b.role, text: b.text, error: b.error }));
    } catch {
      return [];
    }
  }

  private removeStored(id: number): void {
    try {
      sessionStorage.removeItem(this.storageKey(id));
    } catch {
      // nada que limpiar
    }
  }
}
