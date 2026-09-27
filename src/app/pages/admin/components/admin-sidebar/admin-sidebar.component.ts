import { Component, EventEmitter, Input, Output } from '@angular/core';

@Component({
  selector: 'app-admin-sidebar',
  imports: [],
  templateUrl: './admin-sidebar.component.html',
  styleUrl: './admin-sidebar.component.scss',
})
export class AdminSidebarComponent {
  @Input() activeTab: 'products' | 'orders' | 'users' = 'products';

  @Output() tabChanged = new EventEmitter<'products' | 'orders' | 'users'>();

  @Output() logoutClicked = new EventEmitter<void>();

  @Input() adminName = 'Administrador';

  changeTab(tab: 'products' | 'orders' | 'users') {
    this.tabChanged.emit(tab);
  }

  logout() {
    this.logoutClicked.emit();
  }
}
