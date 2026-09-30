import { Component, EventEmitter, Input, Output } from '@angular/core';

@Component({
  selector: 'app-empty-state',
  templateUrl: './empty-state.component.html',
  styleUrl: './empty-state.component.scss',
})
export class EmptyStateComponent {
  @Input() message = '';
  @Input() actionLabel = '';
  @Input() compact = false;

  @Output() action = new EventEmitter<void>();
}