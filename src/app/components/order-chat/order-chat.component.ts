import {
  AfterViewInit,
  Component,
  ElementRef,
  EventEmitter,
  Input,
  OnChanges,
  OnDestroy,
  Output,
  SimpleChanges,
  ViewChild,
  inject,
} from '@angular/core';
import { DatePipe } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { HttpErrorResponse } from '@angular/common/http';
import { Subscription, catchError, filter, of, switchMap, timer } from 'rxjs';
import es from '../../i18n/es.json';
import { LanguageService } from '../../services/language.service';
import { OrderChatService } from '../../services/order-chat.service';
import { ChatRole, OrderChatMessage } from '../../models/order-chat';

const POLL_MS = 3000;
const ES = es as Record<string, string>;

// Panel flotante del chat con el operador. Mismo tamaño y posición que el de Galleguito.
// - Cliente: lo muestra el widget del chatbot dentro de su contenedor.
// - Operador: [docked]="true" lo fija abajo a la derecha por su cuenta.
// No oscurece la pantalla: es un diálogo no modal (Esc o la X lo cierran).
@Component({
  selector: 'app-order-chat',
  imports: [DatePipe, FormsModule],
  templateUrl: './order-chat.component.html',
  styleUrl: './order-chat.component.scss',
  host: { '[class.order-chat-docked]': 'docked' },
})
export class OrderChatComponent implements OnChanges, AfterViewInit, OnDestroy {
  private chatService = inject(OrderChatService);
  private language = inject(LanguageService);

  @Input({ required: true }) orderId!: number;
  @Input({ required: true }) role!: ChatRole;
  // Pedidos con chat para el selector del encabezado (solo si hay más de uno)
  @Input() orderOptions: number[] = [];
  @Input() docked = false;

  @Output() closed = new EventEmitter<void>();
  // Último mensaje visto (para quitar el puntito de mensajes nuevos)
  @Output() seen = new EventEmitter<number>();
  @Output() orderChange = new EventEmitter<number>();

  @ViewChild('scrollBox') scrollBox?: ElementRef<HTMLDivElement>;
  @ViewChild('input') inputEl?: ElementRef<HTMLInputElement>;

  readonly maxLength = 500;

  messages: OrderChatMessage[] = [];
  chatOpen = true;
  draft = '';
  sending = false;
  errorMessage = '';
  loadFailures = 0;

  private lastId?: number;
  private poll?: Subscription;

  get baseId(): string {
    return `order-chat-${this.orderId}`;
  }

  ngOnChanges(changes: SimpleChanges) {
    if (changes['orderId']) {
      this.restart();
    }
  }

  ngAfterViewInit() {
    setTimeout(() => this.inputEl?.nativeElement.focus());
  }

  ngOnDestroy() {
    this.poll?.unsubscribe();
  }

  tr(key: string, params: Record<string, string | number> = {}): string {
    let text = this.role === 'OPERATOR' ? (ES[key] ?? key) : this.language.t(key);
    for (const [name, value] of Object.entries(params)) {
      text = text.replace(`{${name}}`, String(value));
    }
    return text;
  }

  title(): string {
    return this.role === 'CLIENT'
      ? this.tr('order_chat.client_modal_title')
      : this.tr('order_chat.operator_title');
  }

  inputLabel(): string {
    return this.role === 'CLIENT'
      ? this.tr('order_chat.input_label_client')
      : this.tr('order_chat.input_label_operator');
  }

  isMine(message: OrderChatMessage): boolean {
    return message.senderRole === this.role;
  }

  senderLabel(message: OrderChatMessage): string {
    if (message.senderRole === 'OPERATOR') {
      return `${this.tr('order_chat.restaurant')} · ${message.senderName}`;
    }
    return message.senderName || this.tr('order_chat.client');
  }

  srIntro(message: OrderChatMessage): string {
    return this.isMine(message)
      ? this.tr('order_chat.you_said')
      : this.tr('order_chat.other_said', { name: this.senderLabel(message) });
  }

  send() {
    const text = this.draft.trim();
    if (!text || this.sending || !this.chatOpen) return;

    this.sending = true;
    this.errorMessage = '';

    this.chatService.sendMessage(this.orderId, text).subscribe({
      next: (message) => {
        this.draft = '';
        this.sending = false;
        this.addMessages([message]);
      },
      error: (error) => {
        this.sending = false;
        if (error instanceof HttpErrorResponse && error.status === 409) {
          this.chatOpen = false;
          return;
        }
        this.errorMessage = this.tr('order_chat.send_error');
      },
    });
  }

  // Al cambiar de pedido se empieza de cero
  private restart() {
    this.poll?.unsubscribe();
    this.messages = [];
    this.lastId = undefined;
    this.chatOpen = true;
    this.errorMessage = '';
    this.loadFailures = 0;

    this.poll = timer(0, POLL_MS)
      .pipe(
        filter(() => document.visibilityState === 'visible'),
        switchMap(() =>
          this.chatService
            .getMessages(this.orderId, this.lastId)
            .pipe(catchError(() => of(null))),
        ),
      )
      .subscribe((chat) => {
        if (!chat) {
          this.loadFailures++;
          return;
        }
        this.loadFailures = 0;
        this.chatOpen = chat.open;
        this.addMessages(chat.messages);
      });
  }

  private addMessages(list: OrderChatMessage[]) {
    const nuevos = list.filter((m) => !this.messages.some((x) => x.id === m.id));
    if (nuevos.length === 0) return;

    const el = this.scrollBox?.nativeElement;
    const nearBottom = !el || el.scrollHeight - el.scrollTop - el.clientHeight < 80;

    this.messages = [...this.messages, ...nuevos].sort((a, b) => a.id - b.id);
    this.lastId = this.messages[this.messages.length - 1].id;
    this.seen.emit(this.lastId);

    if (nearBottom || nuevos.some((m) => this.isMine(m))) {
      this.scrollToBottom();
    }
  }

  private scrollToBottom() {
    setTimeout(() => {
      const el = this.scrollBox?.nativeElement;
      if (!el) return;
      const reduceMotion = window.matchMedia('(prefers-reduced-motion: reduce)').matches;
      el.scrollTo({ top: el.scrollHeight, behavior: reduceMotion ? 'auto' : 'smooth' });
    });
  }
}