import { NgTemplateOutlet } from '@angular/common';
import { Component, Input } from '@angular/core';
import { RouterLink } from '@angular/router';

export type ButtonVariant = 'primary' | 'outline' | 'glass-light' | 'glass-dark';
export type ButtonSize = 'sm' | 'md' | 'lg';

 // El (click) se pone directamente sobre <app-button>: el evento sube solo pa cuando los pongamos
 
@Component({
  selector: 'app-button',
  imports: [RouterLink, NgTemplateOutlet],
  templateUrl: './button.component.html',
  host: {
    '[class.block]': 'fullWidth',
    '[class.w-full]': 'fullWidth',
    '[class.inline-block]': '!fullWidth',
  },
})
export class ButtonComponent {

  // primary: fondo degradado naranja-rojo, texto blanco
  // outline: borde naranja, texto gris, fondo transparente
  // glass-light: fondo blanco translúcido, texto gris oscuro
  // glass-dark: fondo negro translúcido, texto blanco

  @Input() variant: ButtonVariant = 'primary';
  @Input() size: ButtonSize = 'md';
  @Input() type: 'button' | 'submit' | 'reset' = 'button';
  @Input() fullWidth = false;
  @Input() disabled = false;

  // Si se define, se renderiza un <a> con routerLink en lugar de un <button>. 
  @Input() routerLink?: string | any[];
  @Input() queryParams?: Record<string, any>;
 
  get classes(): string {
    return [
      'btn',
      `btn--${this.size}`,
      `btn--${this.variant}`,
      this.fullWidth ? 'btn--full' : '',
    ]
      .filter(Boolean)
      .join(' ');
  }
}