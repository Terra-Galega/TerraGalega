import { TranslatePipe } from '../../../../pipes/translate.pipe';
import { AfterViewInit, Component, ElementRef, OnDestroy, inject } from '@angular/core';
import { SectionHeadingComponent } from '../../../../components/section-heading/section-heading.component';
import { RevealDirective } from '../../../../directives/reveal.directive';

// Opinión de un crítico gastronómico (contenido fijo de la sección).
interface CriticReview {
  quote: string;
  author: string;
  outlet: string;
  // Calificación de 1 a 5 estrellas
  stars: number;
}

// Sección "La crítica opina" del home: cuatro reseñas fijas de críticos gastronómicos.
// Las tarjetas aparecen con un fundido cuando la sección entra en pantalla, una tras otra.
@Component({
  selector: 'app-testimonials-section',
  imports: [SectionHeadingComponent, RevealDirective, TranslatePipe],
  templateUrl: './testimonials-section.component.html',
  styleUrl: './testimonials-section.component.scss',
})
export class TestimonialsSectionComponent implements AfterViewInit, OnDestroy {
  private host = inject(ElementRef<HTMLElement>);
  private observer?: IntersectionObserver;

  // true cuando la sección ya se vio: activa la animación de entrada
  visible = false;

  readonly reviews: CriticReview[] = [
    {
      quote:
        'El pulpo a feira llega como debe: cocido en su punto, cortado con tijera sobre madera y vestido solo con sal gruesa, pimentón de la Vera y un aceite que sabe a costa. Es un plato que no admite atajos, y aquí se nota que no los toman. La cocina entiende que en la tradición gallega el producto manda y el cocinero acompaña.',
      author: 'Lucía Montenegro',
      outlet: 'Revista Mesa y Mantel',
      stars: 5,
    },
    {
      quote:
        'Lo más interesante de Terra Galega es su bodega. El sommelier construye maridajes con criterio: un Albariño salino para las vieiras, un Godello untuoso para la merluza y una Mencía de la Ribeira Sacra que levanta el cordero sin taparlo. Pocas casas en la ciudad tratan el vino gallego con este respeto.',
      author: 'Andrés Villaveces',
      outlet: 'Guía Sabores de Bogotá',
      stars: 5,
    },
    {
      quote:
        'El cochinillo, de piel crujiente y carne que se deshace, justifica por sí solo la visita, aunque los guisos de cuchara son la verdadera sorpresa. El servicio es atento sin ser invasivo y los salones, cálidos y bien iluminados, invitan a quedarse. Un precio exigente, pero coherente con lo que se sirve.',
      author: 'Marcela Ordóñez',
      outlet: 'Crónica Gastronómica',
      stars: 4,
    },
    {
      quote:
        'La tarta de Santiago, húmeda y con la almendra bien tostada, cierra una comida que empieza con un caldo gallego de los de antes. Terra Galega no intenta reinventar Galicia: la traduce con oficio para Bogotá. Es ya una de las referencias de la cocina española en la ciudad y una mesa obligada para las ocasiones especiales.',
      author: 'Julián Esguerra',
      outlet: 'El Paladar Viajero',
      stars: 5,
    },
  ];

  // Cinco posiciones de estrella para pintar la calificación
  readonly starSlots = [1, 2, 3, 4, 5];


  ngAfterViewInit(): void {
    // Sin IntersectionObserver (navegadores antiguos) las tarjetas se muestran directamente
    if (typeof IntersectionObserver === 'undefined') {
      this.visible = true;
      return;
    }
    this.observer = new IntersectionObserver(
      (entries) => {
        if (entries.some((entry) => entry.isIntersecting)) {
          this.visible = true;
          this.observer?.disconnect();
        }
      },
      { threshold: 0.2 },
    );
    this.observer.observe(this.host.nativeElement);
  }

  ngOnDestroy(): void {
    this.observer?.disconnect();
  }
}
