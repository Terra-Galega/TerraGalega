import { Component, Input } from '@angular/core';

export type IconButtonVariant = 'soft' | 'ghost';

@Component({
  selector: 'app-icon-button',
  templateUrl: './icon-button.component.html',
  styleUrl: './icon-button.component.scss',
})
export class IconButtonComponent {
  @Input() variant: IconButtonVariant = 'soft';
  @Input() label = '';
  @Input() type: 'button' | 'submit' | 'reset' = 'button';
  @Input() disabled = false;
}