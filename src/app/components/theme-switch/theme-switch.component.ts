import { Component, Input, inject } from '@angular/core';
import { ThemeService } from '../../services/theme.service';

// Interruptor "Tema oscuro" (claro <-> oscuro).
// - Normal: texto + interruptor (página de la cuenta).
// - compact: solo el interruptor, para la esquina de las barras superiores (admin y operador).
@Component({
  selector: 'app-theme-switch',
  templateUrl: './theme-switch.component.html',
  styleUrl: './theme-switch.component.scss',
})
export class ThemeSwitchComponent {
  @Input() compact = false;

  theme = inject(ThemeService);
}
