import { Component, EventEmitter, Input, Output } from '@angular/core';

export type AdminTab =
  | 'products'
  | 'categories'
  | 'addons'
  | 'orders'
  | 'users'
  | 'operators'
  | 'deliveries';

@Component({
  selector: 'app-admin-sidebar',
  imports: [],
  templateUrl: './admin-sidebar.component.html',
  styleUrl: './admin-sidebar.component.scss',
})
export class AdminSidebarComponent {
  @Input() activeTab: AdminTab = 'products';

  @Output() tabChanged = new EventEmitter<AdminTab>();

  @Output() logoutClicked = new EventEmitter<void>();

  @Input() adminName = 'Administrador';

  changeTab(tab: AdminTab) {
    this.tabChanged.emit(tab);
  }

  logout() {
    this.logoutClicked.emit();
  }
}
