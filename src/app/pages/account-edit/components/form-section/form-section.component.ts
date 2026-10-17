import { Component, Input } from '@angular/core';

@Component({
  selector: 'app-form-section',
  templateUrl: './form-section.component.html',
  host: { class: 'block' },
})
export class FormSectionComponent {
  @Input() title = '';
}