import { Component, Input } from '@angular/core';
import { TranslatePipe } from '../../pipes/translate.pipe';

// Guía de pasos para visitantes sin sesión. Por defecto muestra "cómo pedir" (página de pedidos);
// otras páginas, como reservas, pasan sus propios pasos con [steps].
@Component({
  selector: 'app-order-steps',
  imports: [TranslatePipe],
  templateUrl: './order-steps.component.html',
  styleUrl: './order-steps.component.scss'
})
export class OrderStepsComponent {

  @Input() steps: { number: number; title: string; description: string; color: string }[] = [
    {
      number: 1,
      title: 'Crea tu cuenta',
      description:
        'Solo necesitamos tu nombre, tu correo y un teléfono de contacto para coordinar la entrega.',
      color: 'terra'
    },
    {
      number: 2,
      title: 'Arma tu pedido',
      description:
        'Entra a la carta, elige tus platos y súmales los adicionales que quieras. Todo queda guardado en el carrito.',
      color: 'terra'
    },
    {
      number: 3,
      title: 'Confirma y paga',
      description:
        'Revisa el total, escoge cómo pagar y confirma. Recibirás el resumen del pedido en tu correo.',
      color: 'terra'
    },
    {
      number: 4,
      title: 'Sigue tu domicilio',
      description:
        'Desde esta misma página verás el estado de cada pedido y el historial completo de lo que has ordenado.',
      color: 'forest'
    }
  ];
}
