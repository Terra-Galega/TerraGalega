import { NgTemplateOutlet } from '@angular/common';
import {
  AfterViewInit,
  Component,
  ElementRef,
  EventEmitter,
  Input,
  NgZone,
  OnDestroy,
  Output,
  ViewChild,
  inject,
} from '@angular/core';
import { GOOGLE_CLIENT_ID } from '../../google.config';

// API mínima de Google Identity Services que se usa aquí
declare global {
  interface Window {
    google?: {
      accounts: {
        id: {
          initialize(options: {
            client_id: string;
            callback: (response: { credential: string }) => void;
            ux_mode?: 'popup' | 'redirect';
          }): void;
          renderButton(element: HTMLElement, options: Record<string, unknown>): void;
        };
      };
    };
  }
}

const GIS_SRC = 'https://accounts.google.com/gsi/client';
let gisLoading: Promise<void> | null = null;

// Carga el script de Google una sola vez para toda la app
function loadGoogleScript(): Promise<void> {
  if (window.google?.accounts?.id) return Promise.resolve();
  gisLoading ??= new Promise<void>((resolve, reject) => {
    const script = document.createElement('script');
    script.src = GIS_SRC;
    script.async = true;
    script.defer = true;
    script.onload = () => resolve();
    script.onerror = () => {
      gisLoading = null;
      reject(new Error('No se pudo cargar Google'));
    };
    document.head.appendChild(script);
  });
  return gisLoading;
}

// Botón oficial "Continuar con Google". Google abre su ventana, el usuario
// elige su cuenta y se emite el ID token (credential) para enviarlo al back.
@Component({
  selector: 'app-google-button',
  imports: [NgTemplateOutlet],
  template: `
    @if (enabled) {
      <!-- Separador "o": antes del botón si va debajo del formulario, después si va encima -->
      @if (position === 'bottom') {
        <ng-container *ngTemplateOutlet="divider"></ng-container>
      }
      <!-- Ocupa todo el ancho del formulario; el botón se dibuja a ese ancho -->
      <div #host class="w-full flex justify-center min-h-[44px]"></div>
      @if (loadError) {
        <p class="text-sm text-center mt-2" style="color: #8a2222">{{ loadError }}</p>
      }
      @if (position === 'top') {
        <ng-container *ngTemplateOutlet="divider"></ng-container>
      }
    }
    <ng-template #divider>
      <div class="flex items-center gap-3 my-6" aria-hidden="true">
        <span class="flex-1 h-px" style="background: rgba(44, 44, 44, 0.12)"></span>
        <span class="text-xs uppercase tracking-wider" style="color: rgba(44, 44, 44, 0.45)">o</span>
        <span class="flex-1 h-px" style="background: rgba(44, 44, 44, 0.12)"></span>
      </div>
    </ng-template>
  `,
})
export class GoogleButtonComponent implements AfterViewInit, OnDestroy {
  private zone = inject(NgZone);

  // 'signup' muestra "Registrarse con Google"; 'login', "Continuar con Google"
  @Input() mode: 'login' | 'signup' = 'login';
  // 'top': el botón va encima del formulario (registro); 'bottom': debajo (inicio de sesión)
  @Input() position: 'top' | 'bottom' = 'bottom';
  @Output() credential = new EventEmitter<string>();

  @ViewChild('host') host?: ElementRef<HTMLElement>;

  enabled = GOOGLE_CLIENT_ID.trim() !== '';
  loadError = '';

  // Ancho con el que se dibujó el botón por última vez
  private renderedWidth = 0;
  private resizeObserver?: ResizeObserver;
  private resizeTimer?: ReturnType<typeof setTimeout>;

  ngAfterViewInit(): void {
    if (!this.enabled) return;

    loadGoogleScript()
      .then(() => {
        const element = this.host?.nativeElement;
        const id = window.google?.accounts.id;
        if (!element || !id) return;

        id.initialize({
          client_id: GOOGLE_CLIENT_ID,
          ux_mode: 'popup',
          // Google llama fuera de Angular: se vuelve a entrar para actualizar la vista
          callback: (response) =>
            this.zone.run(() => this.credential.emit(response.credential)),
        });
        this.render();

        // El botón de Google tiene un ancho fijo en píxeles: cuando el formulario cambia de
        // ancho (girar el móvil, cambiar el tamaño de la ventana) se vuelve a dibujar.
        this.zone.runOutsideAngular(() => {
          this.resizeObserver = new ResizeObserver(() => {
            clearTimeout(this.resizeTimer);
            this.resizeTimer = setTimeout(() => this.render(), 150);
          });
          this.resizeObserver.observe(element);
        });
      })
      .catch(() =>
        this.zone.run(
          () => (this.loadError = 'No se pudo cargar el botón de Google. Revisa tu conexión.'),
        ),
      );
  }

  ngOnDestroy(): void {
    this.resizeObserver?.disconnect();
    clearTimeout(this.resizeTimer);
  }

  // Dibuja el botón al ancho del formulario. Google no admite menos de 200 px ni más de 400 px.
  private render(): void {
    const element = this.host?.nativeElement;
    const id = window.google?.accounts.id;
    if (!element || !id) return;

    const width = Math.round(Math.min(Math.max(element.clientWidth, 200), 400));
    if (width === this.renderedWidth) return;
    this.renderedWidth = width;

    element.replaceChildren();
    id.renderButton(element, {
      type: 'standard',
      theme: 'outline',
      size: 'large',
      shape: 'pill',
      text: this.mode === 'signup' ? 'signup_with' : 'continue_with',
      locale: 'es',
      width,
    });
  }
}
