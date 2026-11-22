import { TranslatePipe } from '../../../pipes/translate.pipe';
import { Component, DestroyRef, HostListener, OnInit, inject } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { ActivatedRoute, Router } from '@angular/router';
import { Subscription, timer } from 'rxjs';
import { NavbarComponent } from '../../../components/navbar/navbar.component';
import { FooterComponent } from '../../../components/footer/footer.component';
import { PageHeaderComponent } from '../../../components/page-header/page-header.component';
import { ButtonComponent } from '../../../components/button/button.component';
import { Reservation } from '../../../models/reservation';
import { ReservationService } from '../../../services/reservation.service';
import { StripeReturnService } from '../../../services/stripe-return.service';
import { ToastService } from '../../../services/toast.service';
import { TutorialReservationService } from '../../../services/tutorial-reservation.service';
import { getErrorMessage } from '../../../utils/http-error';

// Cada cuánto se pregunta el estado mientras Stripe confirma el pago
const POLL_MS = 2000;
// Cuántas veces se pregunta antes de dejar que el cliente lo intente a mano
const MAX_POLLS = 15;

// Lo que se le muestra al cliente
type View =
  | 'checking' // consultando el pago
  | 'paid' // pago aprobado: la reserva quedó confirmada
  | 'abandoned' // volvió de Stripe sin pagar (la sesión sigue abierta)
  | 'waiting' // pagó pero Stripe aún no lo confirma
  | 'expired' // la sesión caducó: la reserva se canceló y la mesa se liberó
  | 'error';

// Página a la que vuelve Stripe tras pagar el anticipo de la reserva:
// /reservations/result?reservation=ID[&canceled=true].
// Consulta el pago al back (que pregunta a Stripe); si se aprobó, la reserva queda confirmada.
// Si el cliente venía del tutorial "hacer una reserva", aquí se retoma.
@Component({
  selector: 'app-reservation-result',
  imports: [NavbarComponent, FooterComponent, PageHeaderComponent, ButtonComponent, TranslatePipe],
  templateUrl: './reservation-result.component.html',
})
export class ReservationResultComponent implements OnInit {
  private route = inject(ActivatedRoute);
  private router = inject(Router);
  private destroyRef = inject(DestroyRef);
  private reservationService = inject(ReservationService);
  private toast = inject(ToastService);
  private tutorialReservation = inject(TutorialReservationService);
  private stripeReturn = inject(StripeReturnService);

  view: View = 'checking';
  reservation: Reservation | null = null;
  errorMessage = '';
  busy = false;

  private canceled = false;
  private polls = 0;
  private poll?: Subscription;

  ngOnInit(): void {
    // Se llegó desde Stripe por el camino normal: ya no hace falta el aviso de "Atrás"
    this.stripeReturn.clear();
    this.canceled = this.route.snapshot.queryParamMap.get('canceled') === 'true';
    this.check();
  }

  // Si se fue otra vez a Stripe ("Volver a pagar") y regresó con "Atrás", el navegador
  // puede mostrar esta página guardada en memoria: se vuelve a consultar el pago.
  @HostListener('window:pageshow', ['$event'])
  onPageShow(event: PageTransitionEvent): void {
    if (event.persisted) {
      this.stripeReturn.clear();
      this.busy = false;
      this.check();
    }
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
      // Primero se muestra que el pago se aprobó; el cliente sigue con "Ver mi reserva"
      this.stop('paid');
    } else if (reservation.paymentState === 'EXPIRED') {
      this.toast.error('Reserva cancelada', 'El tiempo para pagar el anticipo terminó.');
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
    if (!this.reservation) return;
    this.reservationService.redirectToStripe(this.reservation);
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

  /** Fecha legible, ej. "sábado, 21 de noviembre de 2026" (se arma sin zona horaria). */
  dateLabel(date: string): string {
    const [y, m, d] = date.split('-').map(Number);
    return new Date(y, m - 1, d).toLocaleDateString('es-CO', {
      weekday: 'long',
      day: 'numeric',
      month: 'long',
      year: 'numeric',
    });
  }

  /** Texto de dinero, ej. "$50.000". */
  money(value: number): string {
    return `$${value.toLocaleString('es-CO')}`;
  }

  private showError(message: string): void {
    this.errorMessage = message;
    this.view = 'error';
  }
}