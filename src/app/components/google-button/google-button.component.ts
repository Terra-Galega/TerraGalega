import {
  AfterViewInit,
  Component,
  ElementRef,
  EventEmitter,
  Input,
  NgZone,
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
  template: `
    @if (enabled) {
      <div class="flex items-center gap-3 my-6" aria-hidden="true">
        <span class="flex-1 h-px" style="background: rgba(44, 44, 44, 0.12)"></span>
        <span class="text-xs uppercase tracking-wider" style="color: rgba(44, 44, 44, 0.45)">o</span>
        <span class="flex-1 h-px" style="background: rgba(44, 44, 44, 0.12)"></span>
      </div>
      <div #host class="flex justify-center min-h-[44px]"></div>
      @if (loadError) {
        <p class="text-sm text-center mt-2" style="color: #8a2222">{{ loadError }}</p>
      }
    }
  `,
})
export class GoogleButtonComponent implements AfterViewInit {
  private zone = inject(NgZone);

  // 'signup' muestra "Registrarse con Google"; 'login', "Continuar con Google"
  @Input() mode: 'login' | 'signup' = 'login';
  @Output() credential = new EventEmitter<string>();

  @ViewChild('host') host?: ElementRef<HTMLElement>;

  enabled = GOOGLE_CLIENT_ID.trim() !== '';
  loadError = '';

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
        id.renderButton(element, {
          type: 'standard',
          theme: 'outline',
          size: 'large',
          shape: 'pill',
          text: this.mode === 'signup' ? 'signup_with' : 'continue_with',
          locale: 'es',
          width: Math.min(element.clientWidth || 320, 400),
        });
      })
      .catch(() =>
        this.zone.run(
          () => (this.loadError = 'No se pudo cargar el botón de Google. Revisa tu conexión.'),
        ),
      );
  }
}
