import { Component, EventEmitter, Input, Output } from '@angular/core';
import { ButtonComponent } from '../../../../components/button/button.component';
import { SearchInputComponent } from '../../../../components/search-input/search-input.component';

@Component({
  selector: 'app-admin-topbar',
  imports: [ButtonComponent, SearchInputComponent],
  templateUrl: './admin-topbar.component.html',
  styleUrl: './admin-topbar.component.scss',
})
export class AdminTopbarComponent {
  @Input() title = 'Productos';
  @Input() showAddButton = true;

  @Output() searchChanged = new EventEmitter<string>();
  @Output() addClicked = new EventEmitter<void>();
}