import { Injectable } from '@angular/core';

@Injectable({ providedIn: 'root' })
export class TooltipService {
  // Selector CSS de los elementos que pueden mostrar tooltip: botones y enlaces con aria-label,
  // o cualquier elemento con el atributo data-tooltip.
  private static readonly SELECTOR =
    'button[aria-label], a[aria-label], [data-tooltip]';
  // Tiempo de espera (en milisegundos) antes de mostrar el tooltip, para evitar que parpadee al pasar el cursor.
  private static readonly DELAY_MS = 350;

  private started = false;
  private current: Element | null = null;
  private tip: HTMLElement | null = null;
  private timer: ReturnType<typeof setTimeout> | null = null;

  // Registra los eventos globales del documento que controlan cuándo mostrar u ocultar el tooltip.
  init() {
    // Sale si ya se inicializó o si no existe 'document' (por ejemplo, renderizado en servidor).
    if (this.started || typeof document === 'undefined') {
      return;
    }
    // Marca el servicio como iniciado.
    this.started = true;

    // Al entrar el cursor en un elemento, se evalúa si debe mostrar tooltip.
    document.addEventListener('mouseover', (e) => this.enter(e.target));
    // Al salir el cursor, se oculta salvo que solo pase a un elemento hijo del mismo botón.
    document.addEventListener('mouseout', (e) => this.leave(e));
    // Al enfocar con teclado (Tab) también se muestra, por accesibilidad.
    document.addEventListener('focusin', (e) => this.enter(e.target));
    // Al perder el foco se oculta.
    document.addEventListener('focusout', () => this.hide());
    // Al hacer clic o desplazar la página el tooltip ya no tiene sentido
    // El parámetro 'true' usa la fase de captura para detectar el evento aunque otro código lo detenga.
    document.addEventListener('click', () => this.hide(), true);
    document.addEventListener('scroll', () => this.hide(), true);
  }

  // Busca, a partir del nodo del evento, el elemento que debe mostrar tooltip (o null si no aplica).
  private findTarget(node: EventTarget | null): Element | null {
    // Descarta objetivos que no son elementos del DOM (por ejemplo, el propio documento).
    if (!(node instanceof Element)) {
      return null;
    }

    // Sube por el árbol del DOM hasta encontrar el ancestro (o el propio nodo) que coincida con el selector.
    const el = node.closest(TooltipService.SELECTOR);
    if (!el) {
      return null;
    }

    // Los botones con texto visible largo no necesitan descripción
    // (los de "+", "−" o "×" sí cuentan como icono)
    // Si el elemento no tiene data-tooltip explícito, se comprueba la longitud de su texto visible.
    if (!el.hasAttribute('data-tooltip')) {
      const text = (el.textContent ?? '').trim();
      // Más de 2 caracteres se considera texto descriptivo, por lo que no se muestra tooltip.
      if (text.length > 2) {
        return null;
      }
    }

    return el;
  }

  // Obtiene el texto del tooltip: prioriza data-tooltip y, si no existe, usa aria-label.
  private textOf(el: Element): string {
    return (
      el.getAttribute('data-tooltip') ??
      el.getAttribute('aria-label') ??
      ''
    ).trim();
  }

  // Gestiona la entrada del cursor o del foco en un elemento y programa la aparición del tooltip.
  private enter(node: EventTarget | null) {
    const el = this.findTarget(node);

    // Si es el mismo elemento que ya está activo, no hay nada que hacer.
    if (el === this.current) {
      return;
    }

    // Oculta cualquier tooltip o temporizador anterior antes de procesar el nuevo elemento.
    this.hide();

    // Si no hay elemento válido o no tiene texto que mostrar, termina.
    if (!el || !this.textOf(el)) {
      return;
    }

    // Guarda el elemento activo y programa mostrar el tooltip tras el retardo configurado.
    this.current = el;
    this.timer = setTimeout(() => this.show(el), TooltipService.DELAY_MS);
  }

  // Gestiona la salida del cursor de un elemento.
  private leave(event: MouseEvent) {
    // Si no hay elemento activo, no hay nada que ocultar.
    if (!this.current) {
      return;
    }

    // relatedTarget es el elemento al que se mueve el cursor.
    const next = event.relatedTarget;
    // Si el cursor sigue dentro del mismo botón (por ejemplo, pasa a un icono interno), no se oculta.
    if (next instanceof Node && this.current.contains(next)) {
      return;
    }

    this.hide();
  }

  // Crea el tooltip, lo posiciona junto al elemento y lo hace visible.
  private show(el: Element) {
    // Cancela si el elemento ya no está en el DOM o si el cursor ya se movió a otro elemento durante la espera.
    if (!el.isConnected || el !== this.current) {
      return;
    }

    // Crea el contenedor del tooltip con el rol ARIA adecuado y el texto a mostrar.
    const tip = document.createElement('div');
    tip.setAttribute('role', 'tooltip');
    tip.textContent = this.textOf(el);
    // Estilos en línea para que el tooltip funcione sin depender de hojas de estilo externas.
    tip.style.cssText = [
      // Posición fija respecto a la ventana, inicialmente en la esquina para poder medirlo.
      'position:fixed',
      'left:0',
      'top:0',
      // Se muestra por encima del resto de elementos.
      'z-index:9999',
      // No intercepta clics ni eventos del ratón.
      'pointer-events:none',
      // Apariencia: tamaño máximo, espaciado, bordes redondeados y colores.
      'max-width:240px',
      'padding:6px 10px',
      'border-radius:8px',
      'background:#2c2c2c',
      'color:#fff',
      'font-size:12px',
      'font-weight:500',
      'line-height:1.3',
      'white-space:nowrap',
      'box-shadow:0 4px 14px rgba(0,0,0,0.25)',
      // Empieza transparente y se vuelve visible con una transición suave.
      'opacity:0',
      'transition:opacity 0.15s',
    ].join(';');

    // Se añade al DOM para poder conocer su tamaño real (offsetWidth/offsetHeight).
    document.body.appendChild(tip);

    // Posición y tamaño del elemento objetivo respecto a la ventana.
    const rect = el.getBoundingClientRect();
    // Separación mínima respecto a los bordes de la pantalla.
    const margin = 8;

    // Arriba del botón; si no cabe, abajo
    let top = rect.top - tip.offsetHeight - 6;
    if (top < margin) {
      top = rect.bottom + 6;
    }

    // Centrado sobre el botón sin salirse de la pantalla
    let left = rect.left + rect.width / 2 - tip.offsetWidth / 2;
    // Limita la posición horizontal entre el margen izquierdo y el borde derecho de la ventana.
    left = Math.max(
      margin,
      Math.min(left, window.innerWidth - tip.offsetWidth - margin),
    );

    // Aplica la posición calculada y hace visible el tooltip.
    tip.style.left = `${left}px`;
    tip.style.top = `${top}px`;
    tip.style.opacity = '1';

    // Guarda la referencia para poder eliminarlo después.
    this.tip = tip;
  }

  // Oculta el tooltip y reinicia el estado interno.
  private hide() {
    // Cancela el temporizador pendiente, si lo hay.
    if (this.timer) {
      clearTimeout(this.timer);
      this.timer = null;
    }

    // Elimina el tooltip del DOM (si existe) y limpia las referencias.
    this.tip?.remove();
    this.tip = null;
    this.current = null;
  }
}