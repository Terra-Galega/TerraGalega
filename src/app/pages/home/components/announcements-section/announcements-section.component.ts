import { TranslatePipe } from '../../../../services/translate.pipe';
import { LanguageService } from '../../../../services/language.service';
import { AfterViewInit, Component, DestroyRef, ElementRef, Input, computed, effect, inject, signal } from '@angular/core';
import { AnnouncementSlide } from '../../../../models/announcement-slide';
import { CelebrationService } from '../../../../services/celebration.service';
import { CelebrationIconComponent } from '../../../../components/celebration/celebration-icon/celebration-icon.component';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { timer } from 'rxjs';
import { Announcement } from '../../../../models/announcement';
import { SectionHeadingComponent } from '../../../../components/section-heading/section-heading.component';
import { RevealDirective } from '../../../../directives/reveal.directive';

// Cada cuánto avanza solo el carrusel (en milisegundos)
const AUTOPLAY_MS = 6_000;

/**
 * Carrusel horizontal del home: celebración del día primero y todos los anuncios publicados. Muestra uno a la vez,
 * avanza solo cada AUTOPLAY_MS, se detiene mientras el mouse está encima (o si el
 * usuario pulsa pausa) y se puede mover con las flechas, los puntos o deslizando el dedo.
 * Si el sistema pide reducir el movimiento, no avanza solo.
 */
@Component({
  selector: 'app-announcements-section',
  imports: [SectionHeadingComponent, RevealDirective, CelebrationIconComponent, TranslatePipe],
  templateUrl: './announcements-section.component.html',
  styleUrl: './announcements-section.component.scss',
})
export class AnnouncementsSectionComponent implements AfterViewInit {
  // Anuncios que llegan del home
  private readonly published = signal<Announcement[]>([]);
  private readonly host = inject(ElementRef<HTMLElement>);
  private readonly destroyRef = inject(DestroyRef);
  private inView = false;
  private readonly celebrations = inject(CelebrationService);
  private readonly language = inject(LanguageService);

  @Input() set announcements(value: Announcement[]) {
    this.published.set(value);
  }

  readonly slides = computed<AnnouncementSlide[]>(() => {
    const published = this.published().map((item): AnnouncementSlide => ({
      key: `announcement:${item.id}`,
      title: item.title,
      description: item.description,
      imageUrl: item.imageUrl,
      celebration: null,
    }));
    const celebration = this.celebrations.current();
    return celebration ? [{
      key: `celebration:${celebration.id}`,
      title: this.language.t('celebrations.' + celebration.id + '.name'),
      description: this.language.t('celebrations.' + celebration.id + '.announcementMessage'),
      imageUrl: null,
      celebration,
    }, ...published] : published;
  });

  private leadingKey: string | null = null;

  // Posición del anuncio visible
  current = 0;
  // true mientras el mouse (o el foco) está sobre el carrusel
  paused = false;
  // true si el usuario pulsó el botón de pausa
  userPaused = false;

  // Si el sistema pide menos movimiento, el carrusel solo cambia con las flechas o puntos
  private reducedMotion =
    typeof matchMedia !== 'undefined' &&
    matchMedia('(prefers-reduced-motion: reduce)').matches;

  // Posición horizontal donde empezó el toque (para detectar el deslizamiento)
  private touchStartX = 0;

  constructor() {
    // Al cambiar de festividad (fecha o vista previa), se presenta el primer anuncio.
    // Una respuesta tardía del backend conserva la celebración en primer lugar.
    effect(() => {
      const slides = this.slides();
      const leadingKey = slides[0]?.key ?? null;
      if (leadingKey !== this.leadingKey || this.current >= slides.length) {
        this.current = 0;
      }
      this.leadingKey = leadingKey;
    });
    // Avance automático; se cancela solo cuando el componente se destruye
    timer(AUTOPLAY_MS, AUTOPLAY_MS)
      .pipe(takeUntilDestroyed())
      .subscribe(() => {
        if (
          this.inView &&
          !this.paused &&
          !this.userPaused &&
          !this.reducedMotion &&
          this.slides().length > 1
        ) {
          this.next();
        }
      });
  }

  ngAfterViewInit(): void {
    // No se pierde la primera diapositiva mientras el usuario sigue en la portada.
    const observer = new IntersectionObserver(([entry]) => {
      if (entry.isIntersecting && !this.inView) this.current = 0;
      this.inView = entry.isIntersecting;
    }, { threshold: 0.15 });
    observer.observe(this.host.nativeElement);
    this.destroyRef.onDestroy(() => observer.disconnect());
  }
  // Anuncio siguiente (al final vuelve al primero)
  next() {
    if (this.slides().length < 2) return;
    this.current = (this.current + 1) % this.slides().length;
  }

  // Anuncio anterior (al inicio salta al último)
  prev() {
    if (this.slides().length < 2) return;
    this.current =
      (this.current - 1 + this.slides().length) % this.slides().length;
  }

  // Deslizar el dedo: a la izquierda avanza, a la derecha retrocede (mínimo 50 px)
  onTouchStart(event: TouchEvent) {
    this.touchStartX = event.changedTouches[0].clientX;
  }

  onTouchEnd(event: TouchEvent) {
    if (this.slides().length < 2) {
      return;
    }
    const delta = event.changedTouches[0].clientX - this.touchStartX;
    if (Math.abs(delta) < 50) {
      return;
    }
    if (delta < 0) {
      this.next();
    } else {
      this.prev();
    }
  }

  // Salta directo a un anuncio (puntos de abajo)
  goTo(index: number) {
    this.current = index;
  }
}
