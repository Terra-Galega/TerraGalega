import {
  Component,
  EventEmitter,
  Input,
  OnInit,
  Output,
  inject,
} from '@angular/core';
import { OrderSummary } from '../../../../models/order';
import { OrderService } from '../../../../services/order.service';
import { DeliveryPersonService } from '../../../../services/delivery-person.service';
import { ButtonComponent } from '../../../../components/button/button.component';
import { IconButtonComponent } from '../../../../components/icon-button/icon-button.component';
import { FormFieldComponent } from '../../../../components/form-field/form-field.component';
import { getErrorMessage } from '../../../../utils/http-error';
import { DeliveryPersonOption } from '../../../../models/deliveryPerson';
@Component({
  selector: 'app-operator-delivery-modal',
  imports: [ButtonComponent, IconButtonComponent, FormFieldComponent],
  templateUrl: './operator-delivery-modal.component.html',
})
export class OperatorDeliveryModalComponent implements OnInit {
  private orderService = inject(OrderService);
  private deliveryPersonService = inject(DeliveryPersonService);

  @Input({ required: true }) order!: OrderSummary;

  @Output() closed = new EventEmitter<void>();

  deliveryPersons: DeliveryPersonOption[] = [];
  selectedId: number | null = null;

  loading = true;
  sending = false;
  errorMessage = '';

  ngOnInit() {
    this.deliveryPersonService.getAvailableDeliveryPersons().subscribe({
      next: (deliveryPersons) => {
        this.deliveryPersons = deliveryPersons;
        this.loading = false;
      },
      error: (error) => {
        console.error('Error loading delivery persons:', error);
        this.errorMessage = 'No se pudieron cargar los domiciliarios.';
        this.loading = false;
      },
    });
  }

  onSelect(event: Event) {
    this.selectedId = Number((event.target as HTMLSelectElement).value) || null;
  }

  send() {
    if (this.sending) {
      return;
    }

    if (this.selectedId === null) {
      this.errorMessage = 'Selecciona un domiciliario.';
      return;
    }

    this.errorMessage = '';
    this.sending = true;

    this.orderService.sendOrder(this.order.id, this.selectedId).subscribe({
      next: () => {
        this.orderService.refresh();
        this.closed.emit();
      },
      error: (error) => {
        console.error('Error sending order:', error);
        this.errorMessage = getErrorMessage(
          error,
          'No se pudo enviar el pedido.',
        );
        this.sending = false;
      },
    });
  }

  close() {
    this.closed.emit();
  }
}
