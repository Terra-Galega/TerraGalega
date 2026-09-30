import { Component, EventEmitter, Input, Output } from '@angular/core';

@Component({
  selector: 'app-check-option',
  templateUrl: './check-option.component.html',
  styleUrl: './check-option.component.scss',
})
export class CheckOptionComponent {
  @Input() label = '';
  @Input() checked = false;

  @Output() changed = new EventEmitter<boolean>();

  onChange(event: Event) {
    this.changed.emit((event.target as HTMLInputElement).checked);
  }
}