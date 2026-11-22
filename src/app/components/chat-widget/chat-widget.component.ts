import {
  Component,
  ElementRef,
  HostListener,
  computed,
  effect,
  inject,
  signal,
  viewChild,
} from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { RouterLink } from '@angular/router';
import { AuthService } from '../../services/auth.service';
import { CartService } from '../../services/cart.service';
import { ChatService } from '../../services/chat.service';
import { ToastService } from '../../services/toast.service';
import { ChatBubble, ChatMessage } from '../../models/chat';
import { LanguageService } from '../../services/language.service';
import { TranslatePipe } from '../../pipes/translate.pipe';
import { OrderChatHubService } from '../../services/order-chat-hub.service';
import { OrderChatComponent } from '../order-chat/order-chat.component';

// Máximo de mensajes que se guardan en sessionStorage
const MAX_STORED = 40;

// Asistente virtual flotante. Solo se muestra a clientes con sesión iniciada.
// El botón flotante abre un menú con dos opciones: Galleguito y, si hay un chat
// activo, el operador. Los dos paneles usan el mismo espacio.
@Component({
  selector: 'app-chat-widget',
  imports: [RouterLink, TranslatePipe, OrderChatComponent],
  templateUrl: './chat-widget.component.html',
  styleUrl: './chat-widget.component.scss',
})
export class ChatWidgetComponent {
  private auth = inject(AuthService);
  private cart = inject(CartService);
  private chat = inject(ChatService);
  private toast = inject(ToastService);
  private host = inject(ElementRef<HTMLElement>);
  readonly language = inject(LanguageService);
  readonly hub = inject(OrderChatHubService);

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

  // Menú de opciones (Galleguito / Operador)
  readonly menuOpen = signal(false);
  readonly operatorOrderId = this.hub.openOrderId;
  readonly hasOperatorChat = computed(() => this.hub.summaries().length > 0);
  readonly anyPanelOpen = computed(() => this.open() || this.operatorOrderId() !== null);

  // Pedidos para el selector del panel del operador (incluye el abierto aunque aún no tenga mensajes)
  readonly chatOrderIds = computed(() => {
    const ids = this.hub.summaries().map((s) => s.orderId);
    const current = this.operatorOrderId();
    if (current !== null && !ids.includes(current)) {
      ids.unshift(current);
    }
    return ids;
  });

  private clientId: number | null = null;
  private messagesEl = viewChild<ElementRef<HTMLElement>>('messages');
  private inputEl = viewChild<ElementRef<HTMLTextAreaElement>>('input');
  private fabEl = viewChild<ElementRef<HTMLButtonElement>>('fab');
  private botOptionEl = viewChild<ElementRef<HTMLButtonElement>>('botOption');

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
        this.menuOpen.set(false);
        this.draft.set('');
        this.bubbles.set(id !== null ? this.loadStored(id) : []);

        if (id !== null) {
          this.hub.start('CLIENT', id);
        } else {
          this.hub.stop();
        }
      }
      this.isClient.set(id !== null);
      this.firstName.set(session?.name ?? '');
    });

    // Si se abre el chat del operador (desde "Mis pedidos"), se cierra Galleguito
    this.hub.opened$.pipe(takeUntilDestroyed()).subscribe(() => {
      this.open.set(false);
      this.menuOpen.set(false);
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

  // Botón flotante: si hay un panel abierto lo cierra; si no, muestra u oculta el menú
  onFabClick(): void {
    if (this.anyPanelOpen()) {
      if (this.open()) this.toggle();
      if (this.operatorOrderId() !== null) this.closeOperator();
      return;
    }
    this.menuOpen.update((v) => !v);
    if (this.menuOpen()) {
      setTimeout(() => this.botOptionEl()?.nativeElement.focus());
    }
  }

  closeMenu(): void {
    this.menuOpen.set(false);
    this.fabEl()?.nativeElement.focus();
  }

  // Clic fuera del widget: se cierra el menú
  @HostListener('document:click', ['$event'])
  onDocumentClick(event: MouseEvent): void {
    if (this.menuOpen() && !this.host.nativeElement.contains(event.target as Node)) {
      this.menuOpen.set(false);
    }
  }

  openBot(): void {
    this.menuOpen.set(false);
    this.hub.close();
    this.open.set(true);
    setTimeout(() => this.inputEl()?.nativeElement.focus());
  }

  // Abre el chat con mensajes nuevos, o el más reciente
  openOperator(): void {
    this.menuOpen.set(false);
    const list = this.hub.summaries();
    const target = list.find((s) => this.hub.isUnread(s.orderId)) ?? list[0];
    if (target) {
      this.hub.open(target.orderId, this.fabEl()?.nativeElement ?? null);
    }
  }

  closeOperator(): void {
    const opener = this.hub.opener;
    this.hub.close();
    setTimeout(() => {
      const target = opener && opener.isConnected ? opener : this.fabEl()?.nativeElement;
      target?.focus();
    });
  }

  // Cierra a Galleguito (X del encabezado o Esc) y devuelve el foco al botón flotante
  toggle(): void {
    this.open.set(false);
    setTimeout(() => this.fabEl()?.nativeElement.focus());
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
        userBubble.error = true;
        this.bubbles.update((list) => [...list, { role: 'model', text: msg, error: true }]);
        this.sending.set(false);
      },
    });
  }

  pay(url: string | null): void {
    if (url) {
      window.location.href = url;
    }
  }

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