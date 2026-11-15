import {
  AfterViewInit,
  Component,
  ElementRef,
  HostListener,
  OnDestroy,
  ViewChild,
  inject,
} from '@angular/core';
import { ButtonComponent } from '../../../../components/button/button.component';
import { HeroLogoService } from '../../../../services/hero-logo.service';

// Caja que ocupa el texto "Terra Galega" dentro del dibujo del logo (en fracciones del
// viewBox de logo-hero-text.svg). Es lo que se lleva hasta el logo de la navbar.
const TEXT_BOX = { top: 0.136, bottom: 1, left: 0, right: 1 };

// Sección de portada (hero) del home.
// Al bajar, el logo grande sube y se encoge hasta el sitio del logo de la navbar: el pulpo
// se desvanece en la primera mitad del recorrido y, al llegar, el texto se cambia por el
// logo de la barra.
@Component({
  selector: 'app-hero-section',
  // ButtonComponent se usa para los botones Explorar Menú y Nuestra Historia.
  imports: [ButtonComponent],
  templateUrl: './hero-section.component.html',
  styleUrl: './hero-section.component.scss',
})
export class HeroSectionComponent implements AfterViewInit, OnDestroy {
  private heroLogo = inject(HeroLogoService);

  // Sitio del logo en la portada (no se transforma; sirve para medir)
  @ViewChild('logoBox') logoBox!: ElementRef<HTMLElement>;
  // Capa que se mueve hacia la navbar
  @ViewChild('logoMove') logoMove!: ElementRef<HTMLElement>;
  // Capa del pulpo, que se desvanece
  @ViewChild('octopus') octopus!: ElementRef<HTMLElement>;

  private frame = 0;
  private readonly reducedMotion =
    typeof window !== 'undefined' &&
    window.matchMedia('(prefers-reduced-motion: reduce)').matches;

  ngAfterViewInit(): void {
    this.heroLogo.docked.set(false);
    this.update();
  }

  // Un solo cálculo por fotograma, aunque lleguen varios eventos de scroll
  @HostListener('window:scroll')
  @HostListener('window:resize')
  onScroll(): void {
    if (this.frame) return;
    this.frame = requestAnimationFrame(() => {
      this.frame = 0;
      this.update();
    });
  }

  ngOnDestroy(): void {
    cancelAnimationFrame(this.frame);
    // Fuera del home la navbar siempre muestra su logo
    this.heroLogo.docked.set(true);
  }

  private update(): void {
    // Recorrido del efecto: algo menos de media pantalla de scroll
    const distance = Math.min(window.innerHeight * 0.45, 420);
    const p = Math.min(Math.max(window.scrollY / distance, 0), 1);

    // Al llegar al final, el logo pasa a la navbar
    const docked = p >= 1;
    if (this.heroLogo.docked() !== docked) this.heroLogo.docked.set(docked);

    const move = this.logoMove.nativeElement;
    const octopus = this.octopus.nativeElement;

    if (this.reducedMotion) {
      move.style.transform = '';
      move.style.opacity = '';
      octopus.style.opacity = '';
      return;
    }

    // Logo de la navbar que esté visible (escritorio o móvil)
    const target = Array.from(
      document.querySelectorAll<HTMLElement>('#nav-bar .logo-wrap a, #nav-bar .logo-wrap-mobile'),
    )
      .map((el) => el.getBoundingClientRect())
      .find((r) => r.width > 0);

    if (!target || p === 0) {
      move.style.transform = '';
      move.style.opacity = '';
      octopus.style.opacity = '';
      octopus.style.transform = '';
      return;
    }

    // Caja actual del texto del logo, sin el movimiento
    const box = this.logoBox.nativeElement.getBoundingClientRect();
    const text = {
      x: box.left + box.width * TEXT_BOX.left,
      y: box.top + box.height * TEXT_BOX.top,
      w: box.width * (TEXT_BOX.right - TEXT_BOX.left),
      h: box.height * (TEXT_BOX.bottom - TEXT_BOX.top),
    };

    // Escala final para que el texto quepa en el sitio del logo de la barra
    const endScale = Math.min(target.width / text.w, target.height / text.h);
    const scale = 1 + (endScale - 1) * p;

    // El centro del texto viaja en línea recta hasta el centro del logo de la barra
    const fromX = text.x + text.w / 2;
    const fromY = text.y + text.h / 2;
    const toX = target.left + target.width / 2;
    const toY = target.top + target.height / 2;
    const cx = fromX + (toX - fromX) * p;
    const cy = fromY + (toY - fromY) * p;

    // Con el origen arriba a la izquierda de la caja, el centro del texto queda en
    // box + (centro - box) * escala; se traslada para que caiga en (cx, cy).
    const tx = cx - (box.left + (fromX - box.left) * scale);
    const ty = cy - (box.top + (fromY - box.top) * scale);
    move.style.transform = `translate3d(${tx}px, ${ty}px, 0) scale(${scale})`;

    // Al llegar, el texto se funde con el logo de la navbar (transición en el CSS)
    move.style.opacity = docked ? '0' : '1';

    // El pulpo se desvanece en la primera mitad, subiendo un poco y girando apenas
    const o = Math.min(p / 0.5, 1);
    octopus.style.opacity = String(1 - o);
    octopus.style.transform = `translate3d(${o * 6}%, ${-o * 10}%, 0) rotate(${o * 8}deg) scale(${1 + o * 0.08})`;
  }
}
