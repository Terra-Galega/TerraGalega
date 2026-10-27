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
    // Consulta los domiciliarios disponibles al inicializar el modal para permitir asignar el pedido.
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
    // Convierte el valor del selector a número y utiliza null si no hay un identificador válido.
    this.selectedId = Number((event.target as HTMLSelectElement).value) || null;
  }

  send() {
    // Evita enviar varias solicitudes simultáneas mientras ya se procesa una asignación.
    if (this.sending) {
      return;
    }

    // Impide enviar el pedido si todavía no se ha seleccionado un domiciliario.
    if (this.selectedId === null) {
      this.errorMessage = 'Selecciona un domiciliario.';
      return;
    }

    this.errorMessage = '';
    this.sending = true;

    // Envía al servicio el identificador del pedido y el domiciliario seleccionado para realizar la asignación.
    this.orderService.sendOrder(this.order.id, this.selectedId).subscribe({
      next: () => {
        // Notifica al servicio que debe actualizar los datos de los pedidos para reflejar la asignación realizada.
        this.orderService.refresh();
        // Emite el evento de cierre para que el componente padre cierre el modal tras completar la operación.
        this.closed.emit();
      },
      error: (error) => {
        console.error('Error sending order:', error);
        // Obtiene un mensaje adecuado para el error y permite volver a intentar el envío.
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
