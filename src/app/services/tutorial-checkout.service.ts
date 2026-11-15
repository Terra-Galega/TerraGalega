import { Injectable, inject } from '@angular/core';
import { TourStep, TutorialService } from './Tutorial.service';

// ¿Existe este elemento en la página ahora mismo?
const has = (selector: string): boolean => !!document.querySelector(selector);

// Servicio auxiliar: tramo del checkout dentro del tutorial "hacer un pedido".
// Igual que TutorialAuthService, no corre solo: las páginas lo llaman al abrirse
// y solo arranca si el cliente viene del tutorial (flujo 'order' armado).
//
//   /checkout          -> resumeCheckout(): dirección, cupón, método de pago y Continuar
//   /checkout/confirm  -> resumeConfirm():  revisión del pedido; aquí termina el tutorial
@Injectable({ providedIn: 'root' })
export class TutorialCheckoutService {
    private tutorial = inject(TutorialService);

    // true cuando el cliente pulsó «Aplicar» o eligió un cupón durante el tutorial
    private couponTried = false;

    // La llama CheckoutComponent al abrirse (ya con sesión de cliente)
    resumeCheckout(): void {
        if (!this.tutorial.hasFlow('order')) return;
        this.couponTried = false;
        void this.tutorial.run(this.checkoutSteps());
    }

    // La llama CheckoutConfirmComponent al abrirse. Es el último tramo: se cierra
    // el flujo para que «Editar» (volver al paso 1) no reinicie el tutorial.
    resumeConfirm(): void {
        if (!this.tutorial.hasFlow('order')) return;
        this.tutorial.endFlow();
        void this.tutorial.run(this.confirmSteps());
    }

    // ---- Paso 1: entrega y pago ----

    private checkoutSteps(): TourStep[] {
        return [
            // Presentación. Espera a que carguen las direcciones guardadas
            // (o el selector para escribir una, si no tiene ninguna).
            {
                ready: '#address-options, #address-entry-mode',
                title: 'Paso 1: entrega y pago',
                description:
                    'Ya casi está. Te acompañamos con la dirección de entrega, el cupón y el método de pago.',
            },

            // ---- Dirección ----

            // Tiene direcciones guardadas: elegir una o «Usar otra dirección»
            {
                element: '#address-options',
                when: () => has('#address-options'),
                title: '¿Dónde lo recibes?',
                description:
                    'Estas son tus direcciones guardadas. Toca la que quieras usar, o «Usar otra dirección» para escribir una nueva.',
                side: 'right',
                align: 'start',
                waitClick: '#address-options label',
            },
            // Dirección nueva: escribirla o ubicarla en el mapa
            {
                element: '#address-entry-mode',
                when: () => has('#address-entry-mode'),
                title: 'Dirección nueva',
                description: 'Puedes escribirla o ubicarla en el mapa. Toca la opción que prefieras.',
                side: 'bottom',
                align: 'center',
                waitClick: '#address-entry-mode button',
            },
            // Eligió escribirla
            {
                element: '#address-input',
                when: () => has('#address-input'),
                title: 'Escribe tu dirección',
                description:
                    'Calle, número y apartamento si aplica. Cuando termines, toca fuera del campo para continuar.',
                side: 'bottom',
                align: 'start',
                waitInput: { selector: '#address-input', valid: (v) => v.trim().length > 0 },
            },
            // Eligió el mapa: avanza cuando aparece la opción de guardar (ya eligió un punto)
            {
                element: '#address-map',
                when: () => has('#address-map'),
                title: 'Ubícala en el mapa',
                description:
                    'Busca tu dirección o marca el punto en el mapa. Cuando la elijas, seguimos solos.',
                side: 'right',
                align: 'center',
                waitElement: '#address-save-option',
                canSkip: true,
            },
            // Guardar la dirección nueva en el perfil
            {
                element: '#address-save-option',
                when: () => has('#address-save-option'),
                title: 'Guárdala para la próxima',
                description:
                    'Viene marcada: la guardaremos en tu perfil para tus próximos pedidos. Si no quieres, desmárcala.',
                side: 'right',
                align: 'center',
            },
            // Nombre con el que se guarda (solo si dejó marcado guardar)
            {
                element: '#address-name',
                when: () => has('#address-name'),
                title: 'Ponle un nombre',
                description:
                    'Por ejemplo «Casa» u «Oficina». Escríbelo y toca fuera del campo. Si lo dejas vacío se llamará «Casa».',
                side: 'bottom',
                align: 'start',
                waitInput: { selector: '#address-name', valid: () => true },
                canSkip: true,
            },

            // ---- Cupón ----

            {
                element: '#checkout-coupon',
                title: 'Cupón de descuento',
                description:
                    'Si tienes un cupón, escribe el código y pulsa «Aplicar», o toca uno de tus cupones. Si no tienes, pulsa «Siguiente».',
                side: 'right',
                align: 'start',
                waitClick: '#btn-apply-coupon, [id^="coupon-option-"]',
                canSkip: true,
                afterClick: () => (this.couponTried = true),
            },
            // Intentó aplicar uno y todavía no queda aplicado: espera la validación.
            // Si el código no sirve, el globo se queda para que lo corrija.
            {
                element: '#checkout-coupon',
                when: () => this.couponTried && !has('#coupon-applied'),
                title: 'Validando tu cupón',
                description:
                    'Si el código no sirve verás el motivo en rojo: corrígelo y vuelve a pulsar «Aplicar», o pulsa «Siguiente» para seguir sin cupón.',
                side: 'right',
                align: 'start',
                waitElement: '#coupon-applied',
                canSkip: true,
            },
            // Cupón aplicado: el descuento se ve en el resumen
            {
                element: '#checkout-summary',
                when: () => has('#coupon-applied'),
                title: '¡Cupón aplicado!',
                description:
                    'El descuento ya se resta en tu resumen. Si cambias de opinión, puedes quitarlo con «Quitar cupón».',
                side: 'left',
                align: 'start',
            },

            // ---- Método de pago ----

            {
                element: '#checkout-payment',
                title: '¿Cómo quieres pagar?',
                description:
                    '«Contra entrega» es en efectivo cuando llegue tu pedido; «Tarjeta» es en la pasarela segura de Stripe. Toca una opción.',
                side: 'top',
                align: 'center',
                waitClick: '#payment-cash, #payment-card',
            },
            // Eligió efectivo
            {
                element: '#payment-cash-info',
                when: () => has('#payment-cash-info'),
                title: 'Pago contra entrega',
                description:
                    'Pagarás el total en efectivo al domiciliario. No tienes que escribir nada más.',
                side: 'top',
                align: 'start',
            },
            // Eligió tarjeta
            {
                element: '#payment-card-info',
                when: () => has('#payment-card-info'),
                title: 'Pago con tarjeta',
                description:
                    'Al confirmar te llevaremos a Stripe para escribir los datos de tu tarjeta. TerraGalega nunca los ve.',
                side: 'top',
                align: 'start',
            },

            // ---- Resumen y continuar ----

            {
                element: '#checkout-summary',
                title: 'Tu resumen',
                description:
                    'Aquí ves tus platos, el subtotal y el descuento. El domicilio se suma en el siguiente paso.',
                side: 'left',
                align: 'start',
            },
            // Espera a que el cliente pase al paso 2. Si falta algo, el formulario
            // lo marca en rojo y el globo se queda aquí.
            {
                element: '#btn-continue',
                title: 'Continúa al paso 2',
                description:
                    'Pulsa «Continuar». Si falta algo te lo marcaremos en rojo; corrígelo y vuelve a intentar.',
                side: 'top',
                align: 'center',
                waitNavigation: true,
            },
        ];
    }

    // ---- Paso 2: revisar y confirmar (fin del tutorial) ----

    private confirmSteps(): TourStep[] {
        return [
            {
                ready: '#btn-confirm-order',
                title: 'Paso 2: revisa tu pedido',
                description: 'Antes de pedir, revisa que todo esté bien.',
            },
            {
                element: '#confirm-delivery',
                title: 'Dirección de entrega',
                description:
                    'Aquí llegará tu pedido. Con «Editar» vuelves al paso 1 sin perder lo que elegiste.',
                side: 'right',
                align: 'center',
            },
            {
                element: '#confirm-coupon',
                when: () => has('#confirm-coupon'),
                title: 'Tu cupón',
                description: 'Este es el cupón aplicado y cuánto ahorras.',
                side: 'right',
                align: 'center',
            },
            {
                element: '#confirm-payment',
                title: 'Método de pago',
                description: 'Así pagarás tu pedido. También puedes cambiarlo con «Editar».',
                side: 'right',
                align: 'center',
            },
            {
                element: '#confirm-summary',
                title: 'Total a pagar',
                description: 'Ahora sí, con el domicilio y el descuento incluidos.',
                side: 'left',
                align: 'start',
            },
            // Último paso: solo se muestra el botón, el tutorial no lo pulsa
            {
                element: '#btn-confirm-order',
                title: '¡Listo para pedir!',
                description:
                    'Cuando quieras, pulsa este botón: con contra entrega tu pedido se crea de una vez; con tarjeta te llevamos a Stripe para pagar. ¡Aquí termina el tutorial!',
                side: 'top',
                align: 'center',
            },
        ];
    }
}