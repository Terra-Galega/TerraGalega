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
import { Observable, Subscription, catchError, filter, of, switchMap, timer } from 'rxjs';
import es from '../../i18n/es.json';
import { LanguageService } from '../../services/language.service';
import { OrderChatService } from '../../services/order-chat.service';
import { ChatRole, OrderChatMessage } from '../../models/order-chat';
import { CHAT_PHOTO_ACCEPT, ChatPhotoError, prepareChatPhoto } from '../../utils/chat-photo';

const POLL_MS = 3000;
const ES = es as Record<string, string>;

// Panel flotante del chat con el operador. Mismo tamaño y posición que el de Galleguito.
// - Cliente: lo muestra el widget del chatbot dentro de su contenedor; puede adjuntar fotos.
// - Operador: [docked]="true" lo fija abajo a la derecha por su cuenta; ve las fotos.
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
  // Nombre de la otra persona del chat (el operador pasa el del cliente del pedido)
  @Input() counterpartName = '';

  @Output() closed = new EventEmitter<void>();
  // Último mensaje visto (para quitar el puntito de mensajes nuevos)
  @Output() seen = new EventEmitter<number>();
  @Output() orderChange = new EventEmitter<number>();

  @ViewChild('scrollBox') scrollBox?: ElementRef<HTMLDivElement>;
  @ViewChild('input') inputEl?: ElementRef<HTMLInputElement>;
  @ViewChild('fileInput') fileInput?: ElementRef<HTMLInputElement>;

  readonly maxLength = 500;
  readonly photoAccept = CHAT_PHOTO_ACCEPT;

  messages: OrderChatMessage[] = [];
  chatOpen = true;
  draft = '';
  sending = false;
  errorMessage = '';
  loadFailures = 0;

  // Foto elegida y aún sin enviar
  pendingPhoto: Blob | null = null;
  pendingPreview: string | null = null;
  preparingPhoto = false;

  // Fotos ya descargadas: id de la foto -> URL local (blob:)
  private photoUrls = new Map<number, string>();
  private loadingPhotos = new Set<number>();
  failedPhotos = new Set<number>();

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
    this.releasePhotos();
    this.clearPending();
  }

  tr(key: string, params: Record<string, string | number> = {}): string {
    let text = this.role === 'OPERATOR' ? (ES[key] ?? key) : this.language.t(key);
    for (const [name, value] of Object.entries(params)) {
      text = text.replace(`{${name}}`, String(value));
    }
    return text;
  }

  // "Chat con {nombre}": el cliente ve el nombre del operador que le responde (en cuanto
  // haya escrito); el operador ve el del cliente del pedido.
  title(): string {
    const other = this.role === 'CLIENT' ? 'OPERATOR' : 'CLIENT';
    const fromMessages = [...this.messages].reverse()
      .find((m) => m.senderRole === other && m.senderName?.trim())?.senderName.trim();
    const name = (this.role === 'OPERATOR' ? this.counterpartName?.trim() : '') || fromMessages;

    if (name) return this.tr('order_chat.chat_with', { name });
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

  // ---------- Fotos ----------

  // Solo el cliente adjunta fotos, y solo con el chat abierto
  canAttach(): boolean {
    return this.role === 'CLIENT' && this.chatOpen;
  }

  canSend(): boolean {
    return (
      this.chatOpen &&
      !this.sending &&
      !this.preparingPhoto &&
      (this.pendingPhoto !== null || this.draft.trim().length > 0)
    );
  }

  photoUrl(attachmentId: number): string | null {
    return this.photoUrls.get(attachmentId) ?? null;
  }

  photoAlt(message: OrderChatMessage): string {
    return this.isMine(message)
      ? this.tr('order_chat.photo_alt_mine')
      : this.tr('order_chat.photo_alt_other', { name: this.senderLabel(message) });
  }

  openFilePicker() {
    this.fileInput?.nativeElement.click();
  }

  async onFileSelected(event: Event) {
    const input = event.target as HTMLInputElement;
    const file = input.files?.[0];
    // Se limpia para poder elegir otra vez la misma foto
    input.value = '';
    if (!file) return;

    this.errorMessage = '';
    this.preparingPhoto = true;
    try {
      const photo = await prepareChatPhoto(file);
      this.clearPending();
      this.pendingPhoto = photo;
      this.pendingPreview = URL.createObjectURL(photo);
      setTimeout(() => this.inputEl?.nativeElement.focus());
    } catch (error) {
      const code = error instanceof ChatPhotoError ? error.code : 'convert';
      this.errorMessage = this.tr(`order_chat.photo_error_${code}`);
    } finally {
      this.preparingPhoto = false;
    }
  }

  removePending() {
    this.clearPending();
    setTimeout(() => this.inputEl?.nativeElement.focus());
  }

  // Al cargar una foto la burbuja crece: si es la última, se baja para verla completa
  onPhotoLoaded(message: OrderChatMessage) {
    if (message.id === this.lastId) {
      this.scrollToBottom();
    }
  }

  // ---------- Enviar ----------

  send() {
    if (!this.canSend()) return;

    const text = this.draft.trim();
    const withPhoto = this.pendingPhoto !== null;
    const request$: Observable<OrderChatMessage> = this.pendingPhoto
      ? this.chatService.sendPhoto(this.orderId, this.pendingPhoto, text)
      : this.chatService.sendMessage(this.orderId, text);

    this.sending = true;
    this.errorMessage = '';

    request$.subscribe({
      next: (message) => {
        this.draft = '';
        this.sending = false;
        this.clearPending();
        this.addMessages([message]);
      },
      error: (error) => {
        this.sending = false;
        if (error instanceof HttpErrorResponse && error.status === 409 && !withPhoto) {
          this.chatOpen = false;
          return;
        }
        this.errorMessage = withPhoto
          ? this.tr('order_chat.photo_error_upload')
          : this.tr('order_chat.send_error');
      },
    });
  }

  // ---------- Internos ----------

  // Al cambiar de pedido se empieza de cero
  private restart() {
    this.poll?.unsubscribe();
    this.releasePhotos();
    this.clearPending();
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

    for (const m of nuevos) {
      if (m.attachmentId) this.loadPhoto(m.attachmentId);
    }

    if (nearBottom || nuevos.some((m) => this.isMine(m))) {
      this.scrollToBottom();
    }
  }

  private loadPhoto(attachmentId: number) {
    if (this.photoUrls.has(attachmentId) || this.loadingPhotos.has(attachmentId)) return;
    this.loadingPhotos.add(attachmentId);
    const orderId = this.orderId;

    this.chatService.getAttachment(orderId, attachmentId).subscribe({
      next: (blob) => {
        this.loadingPhotos.delete(attachmentId);
        // Si mientras tanto se cambió de pedido, se descarta
        if (orderId !== this.orderId) return;
        this.photoUrls.set(attachmentId, URL.createObjectURL(blob));
      },
      error: () => {
        this.loadingPhotos.delete(attachmentId);
        this.failedPhotos.add(attachmentId);
      },
    });
  }

  private releasePhotos() {
    this.photoUrls.forEach((url) => URL.revokeObjectURL(url));
    this.photoUrls.clear();
    this.loadingPhotos.clear();
    this.failedPhotos.clear();
  }

  private clearPending() {
    if (this.pendingPreview) {
      URL.revokeObjectURL(this.pendingPreview);
    }
    this.pendingPhoto = null;
    this.pendingPreview = null;
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