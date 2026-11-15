import { Injectable, inject } from '@angular/core';
import { TourStep, TutorialService } from './Tutorial.service';

// ¿Existe este elemento en la página ahora mismo?
const has = (selector: string): boolean => !!document.querySelector(selector);



// Cómo está la página de reservas cuando se abre
export type ReservationPageState =
    | 'guest' // sin sesión: se pide entrar o registrarse
    | 'new' // con sesión y sin reserva: plano para crearla
    | 'pending' // ya tiene una reserva con el anticipo sin pagar
    | 'paid'; // ya tiene una reserva confirmada

// Servicio auxiliar: tramos del tutorial "hacer una reserva".
// El primer tramo (clic en «RESERVAS» desde el home) está en TutorialService.stepsFor().
//
//   /reservations         -> resumeReservations(): según el estado de la página
//   /login, /complete-... -> TutorialAuthService (compartido con el pedido)
@Injectable({ providedIn: 'root' })
export class TutorialReservationService {
    private tutorial = inject(TutorialService);

    // La llama ReservationsComponent cuando ya sabe qué mostrar.
    // Si no hay tutorial de reserva pendiente, no hace nada.
    resumeReservations(state: ReservationPageState): void {
        if (!this.tutorial.hasFlow('reservation')) return;

        switch (state) {
            case 'guest':
                void this.tutorial.run(this.guestSteps());
                break;
            case 'new':
                this.tutorial.endFlow();
                void this.tutorial.run(this.planSteps());
                break;
            case 'pending':
                void this.tutorial.run(this.pendingSteps());
                break;
            case 'paid':
                // Último tramo: ya no hay nada más que retomar
                this.tutorial.endFlow();
                void this.tutorial.run(this.paidSteps());
                break;
        }
    }


    // ---- Sin sesión ----

    // El último paso espera el clic en «Iniciar sesión» o «Crear una cuenta». El flujo
    // sigue armado: el login lo retoma y, al entrar, el cliente vuelve a /reservations.
    private guestSteps(): TourStep[] {
        return [
            {
                element: '#reservation-guest-cta',
                title: 'Primero, tu cuenta',
                description:
                    'Para reservar necesitas una cuenta: así guardamos tu mesa y te avisamos de cualquier cambio. Toca «Iniciar sesión» si ya tienes una, o «Crear una cuenta».',
                side: 'top',
                align: 'center',
                waitClick: '#cta-login, #cta-signup',
            },
        ];
    }

    // ---- Plano: crear la reserva ----

    private planSteps(): TourStep[] {
        return [
            // Presentación (globo centrado). Espera a que se dibujen los filtros.
            {
                ready: '#reservation-filters',
                title: 'Reserva tu mesa',
                description:
                    'Te acompañamos paso a paso: fecha, hora, comensales, mesa y anticipo.',
            },

            // ---- Datos de la visita ----

            {
                element: '#reservation-date',
                title: '¿Qué día vienes?',
                description:
                    'Elige la fecha de tu visita (hoy o después). Si es para hoy, pulsa «Siguiente».',
                side: 'bottom',
                align: 'start',
                waitInput: { selector: '#reservation-date', valid: (v) => v.length > 0 },
                canSkip: true,
            },
            {
                element: '#reservation-time',
                title: '¿A qué hora?',
                description:
                    'Los turnos son cada hora y media. Elige uno, o pulsa «Siguiente» para dejar el que está.',
                side: 'bottom',
                align: 'center',
                waitInput: { selector: '#reservation-time' },
                canSkip: true,
            },
            {
                element: '#reservation-party',
                title: '¿Cuántos vienen?',
                description:
                    'Elige el número de comensales. El plano marcará las mesas donde no caben. Pulsa «Siguiente» si ya está bien.',
                side: 'bottom',
                align: 'end',
                waitInput: { selector: '#reservation-party' },
                canSkip: true,
            },

            // ---- Plano ----

            {
                element: '#reservation-floors',
                title: 'Elige el piso',
                description:
                    'El piso 1 tiene el salón principal y los ventanales; el piso 2, el balcón y las salas de celebración. Toca un piso, o pulsa «Siguiente».',
                side: 'bottom',
                align: 'end',
                waitClick: '#reservation-floors button',
                canSkip: true,
            },
            // Avanza cuando hay una mesa seleccionada (aria-pressed="true").
            // Las reservadas o sin cupo no se pueden elegir, así que el globo espera.
            {
                element: '#reservation-map',
                title: 'Elige tu mesa',
                description:
                    'Toca una mesa disponible. Las rayadas con candado ya están reservadas y las apagadas no tienen cupo para tu grupo. También puedes cambiar de piso por la escalera.',
                side: 'top',
                align: 'center',
                waitElement: '.table-marker[aria-pressed="true"]',
            },
            {
                element: '#reservation-table-info',
                title: 'Tu mesa',
                description:
                    'Aquí ves dónde queda, cuántos puestos tiene y la hora. Si no te convence, toca otra en el plano.',
                side: 'left',
                align: 'start',
            },
            // Solo en las salas de celebración
            {
                element: '#reservation-seats',
                when: () => has('#reservation-seats'),
                title: 'Sillas de la sala',
                description:
                    'En las salas de celebración eliges cuántas sillas quieres con − y +. Cuando termines, pulsa «Siguiente».',
                side: 'left',
                align: 'center',
            },

            // ---- Datos de la reserva ----

            {
                element: '#reservation-name',
                title: 'Ponle un nombre',
                description:
                    'Por ejemplo «Cena de grado» o tu nombre. Escríbelo y toca fuera del campo.',
                side: 'left',
                align: 'center',
                waitInput: { selector: '#reservation-name', valid: (v) => v.trim().length > 0 },
            },
            {
                element: '#reservation-theme',
                title: 'Temática',
                description:
                    'Si es una ocasión especial, elige la temática y preparamos la mesa para ella. Si no, pulsa «Siguiente».',
                side: 'left',
                align: 'center',
                waitInput: { selector: '#reservation-theme' },
                canSkip: true,
            },
            {
                element: '#reservation-deposit',
                title: 'El anticipo',
                description:
                    'Toda reserva se confirma con un anticipo que no se reembolsa si la eliminas. Marca la casilla para aceptarlo.',
                side: 'left',
                align: 'center',
                waitClick: '#reservation-deposit',
            },
            // Al pulsar, la página crea la reserva y se va a Stripe. Si algo falla, el
            // aviso sale en rojo y el globo se queda aquí.
            {
                element: '#btn-reserve',
                title: 'Reservar y pagar',
                description:
                    'Este es el último paso: al pulsar «Reservar y pagar» pasas a la pasarela segura de Stripe para pagar el anticipo y tu reserva queda confirmada. El tutorial termina aquí. Si el botón está gris, revisa la mesa, el nombre y el anticipo.',
                side: 'left',
                align: 'center',
            },
        ];
    }


    private pendingSteps(): TourStep[] {
        return [
            {
                element: '#my-reservation',
                title: 'Tu reserva está apartada',
                description:
                    'Ya tienes una reserva, pero falta pagar el anticipo. La mesa se guarda unos 30 minutos; si no pagas, se libera sola.',
                side: 'right',
                align: 'center',
            },
            {
                element: '#my-reservation-actions',
                title: '¿Pagas ahora?',
                description:
                    '«Ir a pagar el anticipo» te lleva a Stripe para pagar. «Cancelar reserva» la anula sin ningún cobro.',
                side: 'top',
                align: 'center',
                waitClick: '#my-reservation-actions button',
            },
        ];
    }

    // Reserva confirmada: fin del tutorial
    private paidSteps(): TourStep[] {
        return [
            {
                element: '#my-reservation',
                title: '¡Reserva confirmada!',
                description:
                    'Recibimos tu anticipo. Aquí ves tu mesa, la fecha, la hora, las sillas y la temática.',
                side: 'right',
                align: 'center',
            },
            {
                element: '#my-reservation-actions',
                title: 'Tu reserva, cuando quieras',
                description:
                    '«Editar reserva» te devuelve al plano para cambiar la mesa, la fecha o la hora. «Eliminar reserva» la anula, pero el anticipo no se reembolsa. ¡Aquí termina el tutorial!',
                side: 'top',
                align: 'center',
            },
        ];
    }
}