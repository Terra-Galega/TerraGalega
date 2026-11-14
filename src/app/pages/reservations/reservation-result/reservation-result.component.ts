import { Component, DestroyRef, OnInit, inject } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { ActivatedRoute, Router } from '@angular/router';
import { Subscription, timer } from 'rxjs';
import { NavbarComponent } from '../../../components/navbar/navbar.component';
import { FooterComponent } from '../../../components/footer/footer.component';
import { PageHeaderComponent } from '../../../components/page-header/page-header.component';
import { ButtonComponent } from '../../../components/button/button.component';
import { Reservation } from '../../../models/reservation';
import { ReservationService } from '../../../services/reservation.service';
import { ToastService } from '../../../services/toast.service';
import { getErrorMessage } from '../../../utils/http-error';

// Cada cuánto se pregunta el estado mientras Stripe confirma el pago
const POLL_MS = 2000;
// Cuántas veces se pregunta antes de dejar que el cliente lo intente a mano
const MAX_POLLS = 15;

// Lo que se le muestra al cliente
type View =
  | 'checking' // consultando el pago
  | 'abandoned' // volvió de Stripe sin pagar (la sesión sigue abierta)
  | 'waiting' // pagó pero Stripe aún no lo confirma
  | 'expired' // la sesión caducó: la reserva se canceló y la mesa se liberó
  | 'error';

// Página a la que vuelve Stripe tras pagar el anticipo de la reserva:
// /reservations/result?reservation=ID[&canceled=true].
// Consulta el pago al back (que pregunta a Stripe); si se aprobó, la reserva queda confirmada.
@Component({
  selector: 'app-reservation-result',
  imports: [NavbarComponent, FooterComponent, PageHeaderComponent, ButtonComponent],
  templateUrl: './reservation-result.component.html',
})
export class ReservationResultComponent implements OnInit {
  private route = inject(ActivatedRoute);
  private router = inject(Router);
  private destroyRef = inject(DestroyRef);
  private reservationService = inject(ReservationService);
  private toast = inject(ToastService);

  view: View = 'checking';
  reservation: Reservation | null = null;
  errorMessage = '';
  busy = false;

  private canceled = false;
  private polls = 0;
  private poll?: Subscription;

  ngOnInit(): void {
    this.canceled = this.route.snapshot.queryParamMap.get('canceled') === 'true';
    this.check();
  }

  // Consulta el estado varias veces: el pago puede tardar unos segundos en confirmarse.
  check(): void {
    this.view = 'checking';
    this.polls = 0;
    this.poll?.unsubscribe();
    this.poll = timer(0, POLL_MS)
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe(() => {
        this.polls++;
        this.reservationService.getPayment().subscribe({
          next: (reservation) => this.apply(reservation),
          error: (error) => {
            this.poll?.unsubscribe();
            this.showError(getErrorMessage(error, 'No pudimos consultar el pago de tu reserva.'));
          },
        });
      });
  }

  private apply(reservation: Reservation): void {
    this.reservation = reservation;
    if (reservation.paymentState === 'PAID') {
      this.poll?.unsubscribe();
      this.toast.show('Reserva confirmada', 'Recibimos el anticipo. ¡Te esperamos!');
      this.router.navigate(['/reservations'], { replaceUrl: true });
    } else if (reservation.paymentState === 'EXPIRED') {
      this.stop('expired');
    } else if (this.canceled) {
      // Pulsó "volver" en Stripe: no tiene sentido seguir esperando
      this.stop('abandoned');
    } else if (this.polls >= MAX_POLLS) {
      this.stop('waiting');
    }
  }

  private stop(view: View): void {
    this.poll?.unsubscribe();
    this.view = view;
  }

  // Vuelve a la página de pago de Stripe (la sesión sigue abierta)
  payAgain(): void {
    if (this.reservation) this.reservationService.redirectToStripe(this.reservation);
  }

  // Cancela la reserva sin pagar: no se cobra nada y la mesa se libera
  cancelReservation(): void {
    if (this.busy) return;
    this.busy = true;
    this.reservationService.deleteReservation().subscribe({
      next: () => {
        this.busy = false;
        this.toast.show('Reserva cancelada', 'No se hizo ningún cobro.');
        this.goToReservations();
      },
      error: (error) => {
        this.busy = false;
        this.showError(getErrorMessage(error, 'No pudimos cancelar la reserva.'));
      },
    });
  }

  goToReservations(): void {
    this.router.navigate(['/reservations'], { replaceUrl: true });
  }

  private showError(message: string): void {
    this.errorMessage = message;
    this.view = 'error';
  }
}
