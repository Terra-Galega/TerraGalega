import { Component, Input } from '@angular/core';

@Component({
  selector: 'app-page-header',
  templateUrl: './page-header.component.html',
  styles: `
    :host {
      display: block;
    }
  `,
})
export class PageHeaderComponent {
  @Input() eyebrow = '';
  @Input() title = '';
}