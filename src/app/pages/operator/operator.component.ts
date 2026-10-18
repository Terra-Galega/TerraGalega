import { Component } from '@angular/core';
import { Router } from '@angular/router';
import { AuthService } from '../../services/auth.service';
import { Order } from '../../models/order';
import { OperatorTopbarComponent } from './components/operator-topbar/operator-topbar.component';
import { OperatorOrdersComponent } from './components/operator-orders/operator-orders.component';
import { OperatorDeliveryModalComponent } from './components/operator-delivery-modal/operator-delivery-modal.component';

@Component({
  selector: 'app-operator',
  imports: [
    OperatorTopbarComponent,
    OperatorOrdersComponent,
    OperatorDeliveryModalComponent,
  ],
  templateUrl: './operator.component.html',
})
export class OperatorComponent {
  operatorId = 0;
  operatorName = '';

  // Order waiting for a delivery person to be chosen
  orderToSend: Order | null = null;

  constructor(
    private router: Router,
    private auth: AuthService,
  ) {
    const session = this.auth.session;

    if (!session || session.type !== 'operator') {
      this.router.navigate(['/login']);
      return;
    }

    this.operatorId = session.id;
    this.operatorName = `${session.name} ${session.lastName}`.trim();
  }

  openSendModal(order: Order) {
    this.orderToSend = order;
  }

  closeSendModal() {
    this.orderToSend = null;
  }

  logout() {
    this.auth.logout();
    this.router.navigate(['/login']);
  }
}