import { Component, inject } from '@angular/core';
import { RouterOutlet } from '@angular/router';
import { TooltipService } from './services/tooltip.service';
import { AuthService } from './services/auth.service';
import { ThemeService } from './services/theme.service';
import { ToastComponent } from './components/toast/toast.component';
import { ClientDeliveryWatcherService } from './services/client-delivery-watcher.service';
import { focusMainContent } from './services/page-title.strategy';
import { NotificationCenterService } from './services/notification-center.service';

@Component({
  selector: 'app-root',
  imports: [RouterOutlet, ToastComponent],
  templateUrl: './app.component.html',
  styleUrl: './app.component.scss',
})
export class AppComponent {
  constructor() {
    // aplica el tema (claro u oscuro) guardado por el usuario
    inject(ThemeService);

    // activa las descripciones al pasar el cursor sobre los botones de icono
    inject(TooltipService).init();

    // Si eliminan tu cuenta mientras tienes la sesión abierta, te saca al inicio
    inject(AuthService).startSessionWatch();

    // Avisa al cliente cuando su pedido inicia el recorrido y cuando ya va llegando
    // (también llena la campanita de notificaciones de la navbar)
    inject(ClientDeliveryWatcherService).start();

    // Lee las notificaciones guardadas en el back y las muestra en la campanita
    inject(NotificationCenterService);
  }

  // "Saltar al contenido": enfoca el título de la página (o su <main>)
  skipToContent(event: Event): void {
    event.preventDefault();
    focusMainContent();
  }
}
