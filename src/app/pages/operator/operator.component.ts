import { Component } from '@angular/core';
import { Router } from '@angular/router';
import { AuthService } from '../../services/auth.service';
import { OrderSummary } from '../../models/order';
import { OperatorOrderDetailModalComponent } from './components/operator-order-detail-modal/operator-order-detail-modal.component';
import { OperatorTopbarComponent } from './components/operator-topbar/operator-topbar.component';
import { OperatorOrdersComponent } from './components/operator-orders/operator-orders.component';
import { OperatorReservationsComponent } from './components/operator-reservations/operator-reservations.component';
import { OperatorDeliveryModalComponent } from './components/operator-delivery-modal/operator-delivery-modal.component';
import { OperatorService } from '../../services/operator.service';
import { getErrorMessage } from '../../utils/http-error';
import { OperatorTrackingComponent } from './components/operator-tracking/operator-tracking.component';
@Component({
  selector: 'app-operator',
  imports: [
    OperatorTopbarComponent,
    OperatorOrdersComponent,
    OperatorDeliveryModalComponent,
    OperatorOrderDetailModalComponent,
    OperatorTrackingComponent,
    OperatorReservationsComponent,
  ],
  templateUrl: './operator.component.html',
})
export class OperatorComponent {
  // Sección visible del panel
  view: 'orders' | 'reservations' = 'orders';
  operatorId = 0;
  operatorName = '';
  operatorUsername = '';

  // Guarda el pedido seleccionado para iniciar el proceso de envío; null indica que no hay ninguno seleccionado
  orderToSend: OrderSummary | null = null;
  // Guarda el identificador del pedido cuyo detalle se quiere visualizar; null indica que no se muestra ningún detalle
  orderIdToView: number | null = null;

  constructor(
    private router: Router,
    private auth: AuthService,
    private operatorService: OperatorService,
  ) {
    const session = this.auth.session;

    // Comprueba que exista una sesión activa y que el usuario tenga el rol de operador.
    if (!session || session.type !== 'operator') {
      // Redirige al usuario a la pantalla de inicio de sesión si no cumple las condiciones.
      this.router.navigate(['/login']);
      // Detiene la ejecución del constructor para evitar continuar sin una sesión válida.
      return;
    }

    this.operatorId = session.id;
    // Construye el nombre completo uniendo el nombre y el apellido, y elimina los espacios sobrantes de los extremos.
    this.operatorName = `${session.name} ${session.lastName}`.trim();

    // Solicita al servicio la información del operador utilizando su identificador.
    this.operatorService.getMe().subscribe({
      next: (operator) => (this.operatorUsername = operator.username),
      error: (error) => {
        console.error('Error cargando el operador:', error);
        // Redirige a la pantalla de error y reemplaza la entrada actual del historial de navegación.
        this.router.navigate(['/error'], {
          // Evita que la pantalla actual quede como una entrada independiente en el historial de navegación.
          replaceUrl: true,
          // Envía parámetros en la URL para proporcionar información sobre el error.
          queryParams: {
            // Incluye el código de estado HTTP recibido en la respuesta del servidor.
            status: error.status,
            message: getErrorMessage(
              error,
              'No se pudo cargar tu información de operador.',
            ),
          },
        });
      },
    });
  }

  setView(view: 'orders' | 'reservations') {
    this.view = view;
  }

  // Opciones de la barra lateral
  readonly navItems: { id: 'orders' | 'reservations'; label: string; icon: string }[] = [
    { id: 'orders', label: 'Domicilios', icon: 'M3 7h11v9H3zM14 10h4l3 3v3h-7zM7 19.5a1.5 1.5 0 100-3 1.5 1.5 0 000 3zM17 19.5a1.5 1.5 0 100-3 1.5 1.5 0 000 3z' },
    { id: 'reservations', label: 'Reservas', icon: 'M4 5h16v15H4zM4 10h16M9 3v4M15 3v4' },
  ];
  // Abre el modal de envío y guarda el pedido seleccionado para utilizar su información.
  openSendModal(order: OrderSummary) {
    this.orderToSend = order;
  }

  // Abre la vista de detalles guardando el identificador del pedido seleccionado.
  openDetail(orderId: number) {
    this.orderIdToView = orderId;
  }

  // Cierra la vista de detalles al establecer el identificador del pedido en null.
  closeDetail() {
    this.orderIdToView = null;
  }

  // Cierra el modal de envío al eliminar la referencia al pedido seleccionado.
  closeSendModal() {
    this.orderToSend = null;
  }

  // Cierra la sesión del operador y lo redirige a la pantalla de inicio de sesión.
  logout() {
    this.auth.logout();
    this.router.navigate(['/login']);
  }
}
