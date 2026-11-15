import { Directive, ElementRef, OnDestroy, OnInit, inject } from '@angular/core';

// Hace que un bloque aparezca con un fundido suave la primera vez que entra en pantalla.
// Uso: <div appReveal>…</div>. El estilo (.reveal y .reveal--in) está en styles.scss.
@Directive({
  selector: '[appReveal]',
  host: { class: 'reveal' },
})
export class RevealDirective implements OnInit, OnDestroy {
  private el = inject(ElementRef<HTMLElement>);
  private observer?: IntersectionObserver;

  ngOnInit(): void {
    const node: HTMLElement = this.el.nativeElement;
    // Sin soporte, el bloque se muestra directamente
    if (typeof IntersectionObserver === 'undefined') {
      node.classList.add('reveal--in');
      return;
    }
    this.observer = new IntersectionObserver(
      (entries) => {
        if (entries.some((e) => e.isIntersecting)) {
          node.classList.add('reveal--in');
          this.observer?.disconnect();
        }
      },
      { threshold: 0.15, rootMargin: '0px 0px -8% 0px' },
    );
    this.observer.observe(node);
  }

  ngOnDestroy(): void {
    this.observer?.disconnect();
  }
}
