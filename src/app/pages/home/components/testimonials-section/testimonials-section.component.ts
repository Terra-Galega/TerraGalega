import { Component } from '@angular/core';
import { SectionHeadingComponent } from '../../../../components/section-heading/section-heading.component';
import { TestimonialsComponent } from '../testimonial/testimonial.component';

@Component({
  selector: 'app-testimonials-section',
  imports: [SectionHeadingComponent, TestimonialsComponent],
  templateUrl: './testimonials-section.component.html',
  styleUrl: './testimonials-section.component.scss',
})
export class TestimonialsSectionComponent {}