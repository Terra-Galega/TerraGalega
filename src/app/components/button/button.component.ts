import { NgTemplateOutlet } from '@angular/common';
import { AfterViewChecked, Component, ElementRef, Input, inject } from '@angular/core';
import { Params, RouterLink } from '@angular/router';

// Define las variantes visuales permitidas; cada una se corresponde con una clase .btn--<variante> en el SCSS.
export type ButtonVariant =
  | 'primary'
  | 'outline'
  | 'neutral'
  | 'sage'
  | 'danger'
  | 'glass-light'
  | 'glass-dark';
// Define los tamaños disponibles del botón (pequeño, mediano y grande).
export type ButtonSize = 'sm' | 'md' | 'lg';

// El (click) se pone directamente sobre <app-button>: el evento del DOM sube solo.
@Component({
  selector: 'app-button',
  // RouterLink permite renderizar el botón como enlace y NgTemplateOutlet reutiliza el mismo contenido en ambas versiones.
  imports: [RouterLink, NgTemplateOutlet],
  templateUrl: './button.component.html',
  styleUrl: './button.component.scss',
  // Ajusta el contenedor <app-button> según ocupe todo el ancho o solo el de su contenido.
  host: {
    '[class.block]': 'fullWidth',
    '[class.w-full]': 'fullWidth',
    '[class.inline-block]': '!fullWidth',
  },
})
export class ButtonComponent implements AfterViewChecked {
  private host = inject(ElementRef<HTMLElement>).nativeElement as HTMLElement;

  // primary: degradado naranja-rojo, texto blanco
  // outline: borde gris, y al pasar el mouse se llena con el degradado
  // neutral: borde gris, al pasar el mouse solo se oscurece el fondo (Cancelar)
  // sage: verde salvia sólido (Aplicar filtros)
  // glass-light: fondo crema translúcido, texto oscuro (sobre imágenes)
  // glass-dark: fondo blanco translúcido, texto crema (sobre imágenes)
  @Input() variant: ButtonVariant = 'primary';
  // Tamaño del botón; por defecto mediano.
  @Input() size: ButtonSize = 'md';
  // Tipo nativo del botón; por defecto 'button' para que no envíe formularios sin querer.
  @Input() type: 'button' | 'submit' | 'reset' = 'button';
  // Si es true, el botón ocupa todo el ancho disponible de su contenedor.
  @Input() fullWidth = false;
  // Si es true, el botón se deshabilita y no responde a clics.
  @Input() disabled = false;

  // Si se define, se renderiza un <a> con routerLink en lugar de un <button>
  @Input() routerLink?: string | any[];
  // Parámetros de consulta (?clave=valor) que se añaden a la ruta cuando se usa routerLink.
  @Input() queryParams?: Params;

  // Construye la cadena de clases CSS del botón a partir del tamaño, la variante y el ancho completo.
  get classes(): string {
    return (
      [
        'btn',
        `btn--${this.size}`,
        `btn--${this.variant}`,
        this.fullWidth ? 'btn--full' : '',
      ]
        // Descarta los valores vacíos para no dejar clases en blanco.
        .filter(Boolean)
        // Une las clases separadas por espacios, listas para asignarse al atributo class.
        .join(' ')
    );
  }

  // Accesibilidad: el aria-label se escribe en <app-button aria-label="...">, pero quien recibe el
  // foco y lo anuncia el lector de pantalla es el <button>/<a> de dentro. Se pasa ahí (y se quita
  // del contenedor) para que los botones de solo icono —o los que en móvil esconden su texto—
  // tengan nombre. También permite que salga el tooltip de la app (button[aria-label]).
  ngAfterViewChecked(): void {
    const label = this.host.getAttribute('aria-label');
    if (label === null) return;
    const control = this.host.querySelector<HTMLElement>(':scope > button, :scope > a');
    if (!control) return;
    control.setAttribute('aria-label', label);
    this.host.removeAttribute('aria-label');
  }
}
