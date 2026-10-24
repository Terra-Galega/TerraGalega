import { Injectable } from '@angular/core';

// muestra el texto de aria-label (o data-tooltip) cuando el
// cursor se queda sobre un botón o enlace de solo icono, o al enfocarlo con teclado
@Injectable({ providedIn: 'root' })
export class TooltipService {
  private static readonly SELECTOR =
    'button[aria-label], a[aria-label], [data-tooltip]';
  private static readonly DELAY_MS = 350;

  private started = false;
  private current: Element | null = null;
  private tip: HTMLElement | null = null;
  private timer: ReturnType<typeof setTimeout> | null = null;

  init() {
    if (this.started || typeof document === 'undefined') {
      return;
    }
    this.started = true;

    document.addEventListener('mouseover', (e) => this.enter(e.target));
    document.addEventListener('mouseout', (e) => this.leave(e));
    document.addEventListener('focusin', (e) => this.enter(e.target));
    document.addEventListener('focusout', () => this.hide());
    // Al hacer clic o desplazar la página el tooltip ya no tiene sentido
    document.addEventListener('click', () => this.hide(), true);
    document.addEventListener('scroll', () => this.hide(), true);
  }

  private findTarget(node: EventTarget | null): Element | null {
    if (!(node instanceof Element)) {
      return null;
    }

    const el = node.closest(TooltipService.SELECTOR);
    if (!el) {
      return null;
    }

    // Los botones con texto visible largo no necesitan descripción
    // (los de "+", "−" o "×" sí cuentan como icono)
    if (!el.hasAttribute('data-tooltip')) {
      const text = (el.textContent ?? '').trim();
      if (text.length > 2) {
        return null;
      }
    }

    return el;
  }

  private textOf(el: Element): string {
    return (
      el.getAttribute('data-tooltip') ?? el.getAttribute('aria-label') ?? ''
    ).trim();
  }

  private enter(node: EventTarget | null) {
    const el = this.findTarget(node);

    if (el === this.current) {
      return;
    }

    this.hide();

    if (!el || !this.textOf(el)) {
      return;
    }

    this.current = el;
    this.timer = setTimeout(() => this.show(el), TooltipService.DELAY_MS);
  }

  private leave(event: MouseEvent) {
    if (!this.current) {
      return;
    }

    const next = event.relatedTarget;
    if (next instanceof Node && this.current.contains(next)) {
      return;
    }

    this.hide();
  }

  private show(el: Element) {
    if (!el.isConnected || el !== this.current) {
      return;
    }

    const tip = document.createElement('div');
    tip.setAttribute('role', 'tooltip');
    tip.textContent = this.textOf(el);
    tip.style.cssText = [
      'position:fixed',
      'left:0',
      'top:0',
      'z-index:9999',
      'pointer-events:none',
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
      'opacity:0',
      'transition:opacity 0.15s',
    ].join(';');

    document.body.appendChild(tip);

    const rect = el.getBoundingClientRect();
    const margin = 8;

    // Arriba del botón; si no cabe, abajo
    let top = rect.top - tip.offsetHeight - 6;
    if (top < margin) {
      top = rect.bottom + 6;
    }

    // Centrado sobre el botón sin salirse de la pantalla
    let left = rect.left + rect.width / 2 - tip.offsetWidth / 2;
    left = Math.max(
      margin,
      Math.min(left, window.innerWidth - tip.offsetWidth - margin),
    );

    tip.style.left = `${left}px`;
    tip.style.top = `${top}px`;
    tip.style.opacity = '1';

    this.tip = tip;
  }

  private hide() {
    if (this.timer) {
      clearTimeout(this.timer);
      this.timer = null;
    }

    this.tip?.remove();
    this.tip = null;
    this.current = null;
  }
}