import { Component } from '@angular/core';
import { SectionHeadingComponent } from '../../../../components/section-heading/section-heading.component';
import { TestimonialsComponent } from '../testimonial/testimonial.component';

// Sección de opiniones del home: encabezado más las tarjetas de testimonios.
@Component({
  selector: 'app-testimonials-section',
  imports: [SectionHeadingComponent, TestimonialsComponent],
  templateUrl: './testimonials-section.component.html',
  styleUrl: './testimonials-section.component.scss',
})
export class TestimonialsSectionComponent {}