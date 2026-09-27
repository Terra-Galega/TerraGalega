import { Component, EventEmitter, Input, Output } from '@angular/core';

@Component({
  selector: 'app-admin-topbar',
  imports: [],
  templateUrl: './admin-topbar.component.html',
  styleUrl: './admin-topbar.component.scss',
})
export class AdminTopbarComponent {
  @Input() title = 'Productos';

  @Input() showAddButton = true;

  @Output() searchChanged = new EventEmitter<string>();

  @Output() addClicked = new EventEmitter<void>();

  onSearch(event: Event) {
    const input = event.target as HTMLInputElement;
    this.searchChanged.emit(input.value);
  }

  addProduct() {
    this.addClicked.emit();
  }
}
