import { NgTemplateOutlet } from '@angular/common';
import { Component, Input } from '@angular/core';
import { RouterLink } from '@angular/router';

export type ButtonVariant =
  | 'primary'
  | 'outline'
  | 'neutral'
  | 'glass-light'
  | 'glass-dark';
export type ButtonSize = 'sm' | 'md' | 'lg';

// El (click) se pone directamente sobre <app-button>: el evento del DOM sube solo.
@Component({
  selector: 'app-button',
  imports: [RouterLink, NgTemplateOutlet],
  templateUrl: './button.component.html',
  styleUrl: './button.component.scss',
  host: {
    '[class.block]': 'fullWidth',
    '[class.w-full]': 'fullWidth',
    '[class.inline-block]': '!fullWidth',
  },
})
export class ButtonComponent {
  // primary: degradado naranja-rojo, texto blanco
  // outline: borde gris, y al pasar el mouse se llena con el degradado
  // neutral: borde gris, al pasar el mouse solo se oscurece el fondo (Cancelar)
  // glass-light: fondo crema translúcido, texto oscuro (sobre imágenes)
  // glass-dark: fondo blanco translúcido, texto crema (sobre imágenes)
  @Input() variant: ButtonVariant = 'primary';
  @Input() size: ButtonSize = 'md';
  @Input() type: 'button' | 'submit' | 'reset' = 'button';
  @Input() fullWidth = false;
  @Input() disabled = false;

  // Si se define, se renderiza un <a> con routerLink en lugar de un <button>
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