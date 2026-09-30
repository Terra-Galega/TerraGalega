import { Component } from '@angular/core';
import { RouterLink } from '@angular/router';

@Component({
  selector: 'app-story-section',
  imports: [RouterLink],
  templateUrl: './story-section.component.html',
  styleUrl: './story-section.component.scss',
})
export class StorySectionComponent {
  stats = [
    { value: '6+', label: 'Años' },
    { value: '150+', label: 'Platos/día' },
    { value: '4.9★', label: 'Valoración' },
  ];
}