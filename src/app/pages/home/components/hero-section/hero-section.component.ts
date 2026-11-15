import {
  AfterViewInit,
  Component,
  ElementRef,
  HostListener,
  OnDestroy,
  ViewChild,
  inject,
} from '@angular/core';
import { HeroLogoService } from '../../../../services/hero-logo.service';
import { CelebrationSceneComponent } from '../../../../celebrations/celebration-scene/celebration-scene.component';
import { CelebrationSwitcherComponent } from '../../../../celebrations/celebration-switcher/celebration-switcher.component';

// Caja de cada palabra dentro del dibujo del logo, en fracciones del viewBox de
// logo-hero-text.svg (left, top, right, bottom).
const TERRA = { l: 0, t: 0.1362, r: 0.5622, b: 0.5453 };
const GALEGA = { l: 0.057, t: 0.6289, r: 1, b: 1 };
// Lo que hay que mover "Terra" a la derecha para que quede centrada sobre "GALEGA"
const TERRA_CENTER_SHIFT = (GALEGA.l + GALEGA.r) / 2 - (TERRA.l + TERRA.r) / 2;

type Rect = { x: number; y: number; w: number; h: number };

const clamp01 = (v: number) => Math.min(Math.max(v, 0), 1);
// Aceleración y frenado suaves
const ease = (v: number) => (v < 0.5 ? 4 * v * v * v : 1 - Math.pow(-2 * v + 2, 3) / 2);
// Máximo suave entre a y b: igual que Math.max lejos del cruce, pero sin esquina (k: anchura)
const softMax = (a: number, b: number, k: number) => (a + b + Math.sqrt((a - b) ** 2 + k * k)) / 2;
// Avance de una fase que ocupa el tramo [from, to] del recorrido total
const phase = (p: number, from: number, to: number) => ease(clamp01((p - from) / (to - from)));

// Sección de portada (hero) del home.
// Al bajar, el logo grande se convierte en el de la navbar en tres fases encadenadas:
//   1. el pulpo se va (sube, gira un poco y se desvanece),
//   2. "Terra" se desliza hasta quedar centrada sobre "GALEGA",
//   3. las dos palabras suben y se encogen hasta coincidir con las del logo de la barra.
// El avance sigue al scroll con un pequeño suavizado, para que la rueda del ratón no dé saltos.
@Component({
  selector: 'app-hero-section',
  imports: [CelebrationSceneComponent, CelebrationSwitcherComponent],
  templateUrl: './hero-section.component.html',
  styleUrl: './hero-section.component.scss',
})
export class HeroSectionComponent implements AfterViewInit, OnDestroy {
  private heroLogo = inject(HeroLogoService);

  // Sitio del logo en la portada (no se transforma; sirve para medir)
  @ViewChild('logoBox') logoBox!: ElementRef<HTMLElement>;
  // Capa que se funde con el logo de la navbar al final
  @ViewChild('logoFade') logoFade!: ElementRef<HTMLElement>;
  @ViewChild('terra') terra!: ElementRef<HTMLElement>;
  @ViewChild('galega') galega!: ElementRef<HTMLElement>;
  @ViewChild('octopus') octopus!: ElementRef<HTMLElement>;

  // Avance que pide el scroll (target) y avance que se dibuja (current), de 0 a 1
  private target = 0;
  private current = 0;
  private frame = 0;
  private lastTime = 0;

  private readonly reducedMotion =
    typeof window !== 'undefined' &&
    window.matchMedia('(prefers-reduced-motion: reduce)').matches;

  ngAfterViewInit(): void {
    this.heroLogo.docked.set(false);
    this.target = this.current = this.scrollProgress();
    this.render(this.current);
  }

  @HostListener('window:scroll')
  @HostListener('window:resize')
  onScroll(): void {
    this.target = this.scrollProgress();
    if (this.reducedMotion) {
      this.current = this.target;
      this.render(this.current);
      return;
    }
    if (!this.frame) {
      this.lastTime = performance.now();
      this.frame = requestAnimationFrame((t) => this.tick(t));
    }
  }

  ngOnDestroy(): void {
    cancelAnimationFrame(this.frame);
    // Fuera del home la navbar siempre muestra su logo
    this.heroLogo.docked.set(true);
  }

  // Recorrido del efecto: algo menos de media pantalla de scroll
  private scrollProgress(): number {
    const distance = Math.min(window.innerHeight * 0.45, 400);
    return clamp01(window.scrollY / distance);
  }

  // Acerca el avance dibujado al que pide el scroll, igual a cualquier tasa de refresco
  private tick(time: number): void {
    const dt = Math.min(time - this.lastTime, 64);
    this.lastTime = time;
    const k = 1 - Math.pow(1 - 0.16, dt / 16.67);
    this.current += (this.target - this.current) * k;
    if (Math.abs(this.target - this.current) < 0.001) this.current = this.target;

    this.render(this.current);
    this.frame = this.current === this.target ? 0 : requestAnimationFrame((t) => this.tick(t));
  }

  // Dibuja el logo para un avance p (0: en la portada, 1: en la navbar)
  render(p: number): void {
    // Al llegar al final, el logo pasa a la navbar
    const docked = p >= 1;
    if (this.heroLogo.docked() !== docked) this.heroLogo.docked.set(docked);
    this.logoFade.nativeElement.style.opacity = docked ? '0' : '1';

    const terra = this.terra.nativeElement.style;
    const galega = this.galega.nativeElement.style;
    const octopus = this.octopus.nativeElement.style;

    if (this.reducedMotion || p === 0) {
      terra.transform = galega.transform = octopus.transform = '';
      octopus.opacity = '';
      return;
    }

    const box = this.logoBox.nativeElement.getBoundingClientRect();
    const piece = (f: typeof TERRA, shift = 0): Rect => ({
      x: box.left + box.width * (f.l + shift),
      y: box.top + box.height * f.t,
      w: box.width * (f.r - f.l),
      h: box.height * (f.b - f.t),
    });

    // 1. El pulpo se va
    const a = phase(p, 0, 0.32);
    octopus.opacity = String(1 - a);
    octopus.transform = `translate3d(${a * 4}%, ${-a * 12}%, 0) rotate(${a * 12}deg) scale(${1 - a * 0.2})`;

    // 2. "Terra" se centra sobre "GALEGA"
    const b = phase(p, 0.18, 0.5);
    // 3. Las dos palabras suben hasta la navbar
    const c = phase(p, 0.42, 1);

    const nav = this.navTargets();
    const terraRest = piece(TERRA);
    const galegaRest = piece(GALEGA);
    terra.transform = this.flyTo(box, terraRest, piece(TERRA, TERRA_CENTER_SHIFT * b), nav?.terra, c);
    galega.transform = this.flyTo(box, galegaRest, galegaRest, nav?.galega, c);
  }

  // Cajas de "Terra" y "Galega" en el logo de la navbar que esté visible (escritorio o móvil)
  private navTargets(): { terra: DOMRect; galega: DOMRect } | null {
    for (const wrap of Array.from(
      document.querySelectorAll<HTMLElement>('#nav-bar .logo-wrap, #nav-bar .logo-wrap-mobile'),
    )) {
      const terra = wrap.querySelector('.logo-terra')?.getBoundingClientRect();
      const galega = wrap.querySelector('.logo-galega')?.getBoundingClientRect();
      if (terra && galega && terra.width > 0) return { terra, galega };
    }
    return null;
  }

  // Transformación de una pieza (que ocupa toda la caja del logo, con origen arriba a la
  // izquierda). rest: dónde está la palabra sin mover; from: dónde debe verse antes de subir
  // (para "Terra", ya centrada); to: su sitio en la navbar; t: avance de la subida.
  private flyTo(box: DOMRect, rest: Rect, from: Rect, to: DOMRect | undefined, t: number): string {
    const toW = to ? to.width : from.w;
    const toX = to ? to.left + to.width / 2 : from.x + from.w / 2;
    const toY = to ? to.top + to.height / 2 : from.y + from.h / 2;
    const scale = 1 + (toW / from.w - 1) * t;

    // Antes de subir, la palabra se desplaza con la página. Para que no pase por encima de la
    // barra y luego tenga que bajar, su altura de partida nunca queda por encima de la de
    // destino (con un tope suave, para que no frene de golpe).
    const fromX = from.x + from.w / 2;
    const fromY = softMax(from.y + from.h / 2, toY, 90);

    // Centro que debe tener la palabra en este momento
    const cx = fromX + (toX - fromX) * t;
    const cy = fromY + (toY - fromY) * t;

    // Tras escalar desde la esquina de la caja, el centro sin mover queda en
    // box + (centro - box) * escala; se traslada lo que falta para que caiga en (cx, cy).
    const restX = rest.x + rest.w / 2;
    const restY = rest.y + rest.h / 2;
    const tx = cx - (box.left + (restX - box.left) * scale);
    const ty = cy - (box.top + (restY - box.top) * scale);
    return `translate3d(${tx}px, ${ty}px, 0) scale(${scale})`;
  }
}
