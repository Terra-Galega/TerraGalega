import { Component, EventEmitter, Input, Output } from '@angular/core';

// Tipos de secciones disponibles en el panel administrativo.
export type AdminTab =
  | 'dashboard'
  | 'products'
  | 'categories'
  | 'addons'
  | 'coupons'
  | 'announcements'
  | 'reviews'
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
  
  // Sección seleccionada actualmente en el panel.
  @Input() activeTab: AdminTab = 'dashboard';

  // Eventos para comunicar el cambio de sección y la solicitud de cierre de sesión.
  @Output() tabChanged = new EventEmitter<AdminTab>();

  @Output() logoutClicked = new EventEmitter<void>();

  // Nombre del administrador que se muestra en la barra lateral.
  @Input() adminName = 'Administrador';

  // En móvil y tablet la barra es un panel deslizante: open lo muestra y closed pide cerrarlo.
  @Input() open = false;
  @Output() closed = new EventEmitter<void>();

  // Emite la sección seleccionada para que el componente padre actualice la vista.
  changeTab(tab: AdminTab) {
    this.tabChanged.emit(tab);
    this.closed.emit();
  }

  // Emite el evento para que el componente padre gestione el cierre de sesión.
  logout() {
    this.logoutClicked.emit();
  }
}
