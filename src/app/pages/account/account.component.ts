import {
  Component,
  DestroyRef,
  HostListener,
  inject,
  OnInit,
} from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { ActivatedRoute, Router } from '@angular/router';
import { Subscription, catchError, map, of, switchMap, timer } from 'rxjs';
import { NavbarComponent } from '../../components/navbar/navbar.component';
import { FooterComponent } from '../../components/footer/footer.component';
import { PageHeaderComponent } from '../../components/page-header/page-header.component';
import { ProfileCardComponent } from './components/profile-card/profile-card.component';
import { PersonalInfoComponent } from './components/personal-info/personal-info.component';
import { AccountActionsComponent } from './components/account-actions/account-actions.component';
import { AuthService } from '../../services/auth.service';
import { Client } from '../../models/client';
import { ClientService } from '../../services/client.service';
import { SubscriptionStatus } from '../../models/subscription';
import {SubscriptionService,formatSubscriptionDate,} from '../../services/subscription.service';
import { ButtonComponent } from '../../components/button/button.component';
import { ModalComponent } from '../../components/modal/modal.component';
import { SubscriptionModalComponent } from '../../components/subscription-modal/subscription-modal.component';
import { ToastService } from '../../services/toast.service';
import { getErrorMessage } from '../../utils/http-error';
import { AddressListComponent } from './components/address-list/address-list.component';

// Cada cuánto se pregunta el estado mientras Stripe confirma el pago
const POLL_MS = 5000;
// Cuántas veces se pregunta antes de dejar de esperar (15 × 5 s = 75 s)
const MAX_POLLS = 15;
@Component({
  selector: 'app-account',
  imports: [
    NavbarComponent,
    FooterComponent,
    PageHeaderComponent,
    ProfileCardComponent,
    PersonalInfoComponent,
    AccountActionsComponent,
    ButtonComponent,
    ModalComponent,
    SubscriptionModalComponent,
    AccountActionsComponent,
    AddressListComponent,
    ButtonComponent,
  ],
  templateUrl: './account.component.html',
})
export class AccountComponent implements OnInit {
  //Injextar los servicios que se van a usar
  private clientService = inject(ClientService);
  private subscriptionService = inject(SubscriptionService);
  private route = inject(ActivatedRoute);
  private destroyRef = inject(DestroyRef);
  private router = inject(Router);
  private auth = inject(AuthService);
  private toast = inject(ToastService);

  client: Client | null = null;
  //variables para mostrar u ocultar
  showDeleteModal = false;
  showErrorModal = false;
  confirmTitle = 'Eliminar cuenta';
  confirmMessage =
    '¿Seguro que quieres eliminar tu cuenta? Esta acción no se puede deshacer.';
  errorTitle = '';
  errorMessage = '';
  // Estado de la suscripción (precio, días restantes, si puede pagar...). null si no cargó.
  subscription: SubscriptionStatus | null = null;

  // ---- Suscripción Premium ----
  showSubscribeModal = false;
  // true desde que se pulsa "Pagar" hasta salir hacia Stripe: evita pulsar dos veces
  startingSubscription = false;
  // true mientras se confirma el pago al volver de Stripe
  confirmingPayment = false;
  private poll?: Subscription;


    ngOnInit() {
    //si no ha iniciado sesión
    if (!this.auth.session) {
      this.router.navigate(['/login']);
      return;
    }

    // Stripe vuelve con /account?subscription=done (y &canceled=true si se arrepintió)
    const params = this.route.snapshot.queryParamMap;
    const fromStripe = params.get('subscription') === 'done';
    const canceled = params.get('canceled') === 'true';

    if (fromStripe) {
      // Se quita el parámetro de la URL: si recarga no se repite el aviso
      this.router.navigate([], {
        relativeTo: this.route,
        queryParams: {},
        replaceUrl: true,
      });
    }

    this.load();

    if (fromStripe && canceled) {
      this.toast.error(
        'Pago cancelado',
        'No completaste el pago de tu suscripción.',
      );
    } else if (fromStripe) {
      this.waitForPayment();
    }
  }
  
  // Trae el estado de la suscripción y DESPUÉS el cliente. El orden importa:
  // /subscriptions/status asienta primero cualquier pago pendiente, así el perfil
  // (Freemium/Premium) llega ya actualizado.
  private load(): void {
    this.subscriptionService
      .status()
      .pipe(
        // Si falla (p. ej. Stripe no responde) el perfil se muestra igual, solo sin datos de Premium
        catchError((error) => {
          console.error('Error cargando la suscripción:', error);
          return of(null);
        }),
        //traer el cliente (el back lo identifica por el token)
        switchMap((subscription) =>
          this.clientService
            .getMe()
            .pipe(map((client) => ({ subscription, client }))),
        ),
      )
      .subscribe({
        next: ({ subscription, client }) => {
          this.subscription = subscription;
          this.client = client;
        },
        error: (error) => {
          console.error('Error cargando cliente:', error);
          this.auth.logout();
          this.router.navigate(['/login']);
        },
      });
  }

  // ---- Volver de Stripe con el pago hecho ----

  // Pregunta el estado cada 2 s hasta que el pago deje de estar PENDING. El back
  // consulta a Stripe en cada pregunta, así que funciona aunque el webhook no haya llegado.
  private waitForPayment(): void {
    this.confirmingPayment = true;
    let polls = 0;

    this.poll?.unsubscribe();
    this.poll = timer(0, POLL_MS)
      .pipe(
        // switchMap: si una respuesta se demora, no se acumulan peticiones
        switchMap(() => this.subscriptionService.status()),
        takeUntilDestroyed(this.destroyRef),
      )
      .subscribe({
        next: (status) => {
          polls++;

          if (status.paymentState === 'PENDING' && polls < MAX_POLLS) {
            return; // Stripe aún no confirma: se sigue esperando
          }

          this.finishWaiting(status);
        },
        error: (error) => {
          console.error('Error confirmando el pago:', error);
          this.finishWaiting(null);
        },
      });
  }

  // Termina la espera: recarga el perfil (ya Premium) y avisa cómo quedó el pago
  private finishWaiting(status: SubscriptionStatus | null): void {
    this.poll?.unsubscribe();
    this.confirmingPayment = false;
    this.load();

    if (status?.paymentState === 'PAID') {
      this.toast.show(
        '¡Ya eres Premium!',
        `Tus domicilios son gratis hasta el ${formatSubscriptionDate(status.subscriptionEndsAt)}.`,
        5000,
      );
    } else if (status?.paymentState === 'EXPIRED') {
      this.toast.error(
        'El pago caducó',
        'No se hizo ningún cobro. Puedes intentarlo de nuevo.',
      );
    } else {
      this.toast.show(
        'Estamos confirmando tu pago',
        'Tu suscripción se activará en unos minutos. Recarga la página para ver el cambio.',
        6000,
      );
    }
  }

  // Botón Premium de "Acciones"
  openSubscribeModal(): void {
    // La regla real la valida el back; esto solo evita abrir el modal sin sentido
    if (!this.subscription?.canSubscribe || this.confirmingPayment) {
      return;
    }

    this.showSubscribeModal = true;
  }

  closeSubscribeModal(): void {
    // Mientras se abre el pago no se puede cerrar
    if (!this.startingSubscription) {
      this.showSubscribeModal = false;
    }
  }

  // "Pagar": el back abre la sesión de Stripe y el navegador va a su página de pago
  confirmSubscribe(): void {
    if (this.startingSubscription) {
      return;
    }

    this.startingSubscription = true;

    this.subscriptionService.start().subscribe({
      next: (status) => {
        if (!status.checkoutUrl) {
          this.showSubscribeError('No recibimos el enlace de pago. Inténtalo de nuevo.');
          return;
        }

        // startingSubscription queda en true: el botón sigue bloqueado hasta salir de la página
        this.subscriptionService.redirectToStripe(status);
      },
      error: (error) => {
        console.error('Error abriendo el pago de la suscripción:', error);
        this.showSubscribeError(
          getErrorMessage(error, 'Inténtalo de nuevo en unos minutos.'),
        );
      },
    });
  }

  // Cierra el modal y muestra el motivo en el modal de error. Se vuelve a pedir el
  // estado por si la regla cambió (p. ej. el back respondió 409 porque aún es Premium).
  private showSubscribeError(message: string): void {
    this.startingSubscription = false;
    this.showSubscribeModal = false;
    this.errorTitle = 'No se pudo abrir el pago';
    this.errorMessage = message;
    this.showErrorModal = true;
    this.load();
  }

  // Si el cliente vuelve con la flecha "atrás" desde Stripe, el navegador puede
  // restaurar la página tal como estaba (modal abierto y botón bloqueado)
  @HostListener('window:pageshow', ['$event'])
  onPageShow(event: PageTransitionEvent): void {
    if (event.persisted) {
      this.startingSubscription = false;
      this.showSubscribeModal = false;
      this.load();
    }
  }
  //editar perfil
  editProfile() {
    if (!this.client) {
      return;
    }

    this.router.navigate(['/account', 'edit']);
  }
  //cerrar sesion
  logout() {
    this.auth.logout();
    this.router.navigate(['/login']);
  }
  //borrar cuenta
  deleteAccount() {
    if (!this.client) {
      return;
    }

    this.showDeleteModal = true;
  }
  //btn de cancelar
  cancelDelete() {
    this.showDeleteModal = false;
  }
  //confirmar cancelar
  confirmDelete() {
    if (!this.client) {
      return;
    }

    this.showDeleteModal = false;
    // MANDAR AL SERVICE -> BACKK  a  borrar
    this.clientService.deleteMe().subscribe({
      next: () => {
        this.auth.logout();
        this.toast.show('Cuenta eliminada correctamente');
        this.router.navigate(['/home']);
      },
      error: (error) => {
        console.error('Error eliminando cuenta:', error);
        this.errorTitle = 'No se pudo eliminar la cuenta';
        this.errorMessage = getErrorMessage(error);
        this.showErrorModal = true;
      },
    });
  }

  closeErrorModal() {
    this.showErrorModal = false;
    this.errorTitle = '';
    this.errorMessage = '';
  }
}
