import { Component, inject } from '@angular/core';
import { RouterOutlet } from '@angular/router';
import { TooltipService } from './services/tooltip.service';
import { AuthService } from './services/auth.service';
import { ToastComponent } from './components/toast/toast.component';

@Component({
  selector: 'app-root',
  imports: [RouterOutlet, ToastComponent],
  templateUrl: './app.component.html',
  styleUrl: './app.component.scss'
})
export class AppComponent {
  constructor() {

    // activa las descripciones al pasar el cursor sobre los botones de icono
    inject(TooltipService).init();

    // Si eliminan tu cuenta mientras tienes la sesión abierta, te saca al inicio
    inject(AuthService).startSessionWatch();
    
  }
}