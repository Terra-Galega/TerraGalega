import { TranslatePipe } from '../../../../pipes/translate.pipe';
import { Component } from '@angular/core';
import { OrderStepsComponent } from '../../../../components/order-steps/order-steps.component';
import { ReadyToOrderComponent } from '../../../../components/ready-to-order/ready-to-order.component';

// Página de reservas para visitantes sin sesión: explica por qué se necesita una cuenta,
// los pasos para reservar, muestra los espacios del restaurante e invita a entrar o registrarse.
@Component({
  selector: 'app-reservation-guest',
  imports: [OrderStepsComponent, ReadyToOrderComponent, TranslatePipe],
  templateUrl: './reservation-guest.component.html',
  styleUrl: './reservation-guest.component.scss',
})
export class ReservationGuestComponent {
  readonly steps = [
    {
      number: 1,
      title: 'Crea tu cuenta',
      description: 'Con tu cuenta guardamos la reserva y te avisamos de cualquier cambio en tu mesa.',
      color: 'terra',
    },
    {
      number: 2,
      title: 'Elige fecha, hora y mesa',
      description:
        'Indica cuántos comensales vienen y escoge tu mesa en el plano: salón, balcón o sala de celebración.',
      color: 'terra',
    },
    {
      number: 3,
      title: 'Paga el anticipo',
      description:
        'Confirmas la reserva con un anticipo de $50.000 en la pasarela de pago segura. La mesa queda apartada para ti.',
      color: 'terra',
    },
    {
      number: 4,
      title: 'Disfruta tu visita',
      description:
        'Desde esta misma página puedes ver, editar o cancelar tu reserva cuando lo necesites.',
      color: 'forest',
    },
  ];

  // Espacios del restaurante que se pueden reservar
  readonly spaces = [
    {
      name: 'Salón principal',
      caption: 'Mesas junto a los ventanales y mesas de arco contra la pared.',
      image: 'https://images.unsplash.com/photo-1517248135467-4c7edcad34c4?w=900&h=700&fit=crop&auto=format',
    },
    {
      name: 'Balcón',
      caption: 'La terraza del segundo piso, ideal para cenas al aire libre.',
      image: 'https://images.unsplash.com/photo-1559339352-11d035aa65de?w=900&h=700&fit=crop&auto=format',
    },
    {
      name: 'Barra',
      caption: 'Vinos gallegos y tapas sin necesidad de reserva.',
      image: 'https://images.unsplash.com/photo-1514933651103-005eec06c04b?w=900&h=700&fit=crop&auto=format',
    },
    {
      name: 'Salas de celebración',
      caption: 'Dos salas privadas para grupos de 7 a 20 personas.',
      image: 'https://images.unsplash.com/photo-1414235077428-338989a2e8c0?w=900&h=700&fit=crop&auto=format',
    },
  ];
}
