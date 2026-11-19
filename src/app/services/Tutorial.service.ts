import { Injectable, NgZone, inject } from '@angular/core';
import { NavigationEnd, Router } from '@angular/router';
import { driver, Driver, DriveStep } from 'driver.js';
import { filter } from 'rxjs';
import { LanguageService } from './language.service';

// Nombres de los tutoriales disponibles
export type TourName = 'order' | 'reservation';

type Popover = NonNullable<DriveStep['popover']>;

// Cuánto se espera (ms) a un paso opcional antes de saltárselo
const OPTIONAL_WAIT_MS = 2000;
// Cuánto se espera (ms) a un paso obligatorio antes de cancelar el tutorial
const REQUIRED_WAIT_MS = 8000;
// Antes de revisar la condición "when" de un paso se espera un poquito:
// Angular pinta el cambio (p. ej. al elegir tarjeta) justo después del clic
const WHEN_DELAY_MS = 150;

// Páginas por las que puede pasar un tutorial que quedó pendiente (flujo).
// Si el cliente se va a otra, el flujo se descarta.
// '/reservations' también cubre '/reservations/result' (vuelta de Stripe).
const FLOW_ROUTES: Record<TourName, string[]> = {
    order: ['/login', '/complete-profile', '/checkout'],
    reservation: ['/reservations', '/login', '/complete-profile'],
};

// Un paso del tutorial en formato simple; el servicio lo convierte a DriveStep
export interface TourStep {
    // Elemento a resaltar (selector CSS). Sin elemento, el globo sale centrado.
    element?: string;
    // Selector que debe existir antes de mostrar el paso (p. ej. tras cambiar de página).
    // Si no se indica, se espera al propio elemento.
    ready?: string;
    title: string;
    description: string;
    nextBtnText?: string;
    beforeNext?: () => void;
    // Se ejecuta antes de mostrar el paso, para dejar la pantalla lista (p. ej. abrir un
    // panel que está oculto en móvil). Si devuelve una promesa, se espera a que termine.
    beforeShow?: () => void | Promise<void>;
    side?: Popover['side'];
    align?: Popover['align'];
    // Resalta un control sin habilitar sus acciones durante el paso.
    disableActiveInteraction?: boolean;
    // Si se indica, NO hay botón "Siguiente": el paso avanza solo cuando el cliente
    // hace clic en algo que cumpla este selector.
    waitClick?: string;
    // Igual, pero el paso avanza cuando el cliente termina de escribir en un campo
    // (al salir del campo o pulsar Enter). "valid" decide si lo escrito sirve.
    waitInput?: { selector: string; valid?: (value: string) => boolean };
    // Con waitClick o waitInput, muestra además el botón "Siguiente" (por si el campo
    // ya venía lleno, p. ej. con una cuenta de prueba)
    canSkip?: boolean;
    // El paso avanza cuando aparece este elemento en la página (p. ej. la pantalla
    // "Revisa tu correo" tras crear la cuenta). Si el cliente se equivoca y vuelve a
    // intentar, el globo se queda esperando en vez de seguir de largo.
    waitElement?: string;
    // El paso espera a que el cliente se vaya de la página (p. ej. al iniciar sesión).
    // No hay botón "Siguiente": el tutorial se cierra solo cuando el elemento desaparece.
    waitNavigation?: boolean;
    // Se ejecuta justo después del clic o de escribir (por ejemplo, para cerrar un panel).
    // Recibe el elemento tocado o el campo escrito.
    afterClick?: (target: Element) => void;
    // Paso que solo existe a veces (p. ej. adicionales: no todas las categorías los tienen).
    // Si su elemento no aparece, se salta en vez de cancelar el tutorial.
    optional?: boolean;
    // Paso condicional: solo se muestra si esta función devuelve true al llegar a él.
    // Si devuelve false se salta al instante (sin esperar como "optional").
    // Sirve para mostrar caminos distintos según lo que eligió el cliente.
    when?: () => boolean;
}

// Motor de los tutoriales guiados con driver.js.
// Solo hay uno activo a la vez; el primer tramo de cada tutorial se define en stepsFor().
// Funciona entre páginas: el globo sigue activo mientras el router cambia de ruta.
//
// Un tutorial puede continuar en otra página con un servicio auxiliar (TutorialAuthService,
// TutorialCheckoutService, TutorialReservationService): al terminar su tramo, el motor deja
// el "flujo" armado (armFlow) y la página siguiente llama a su auxiliar, que lo retoma con run().
@Injectable({ providedIn: 'root' })
export class TutorialService {
    // Tutorial que se está mostrando ahora (null = ninguno)
    private active: Driver | null = null;
    // Pasos del tutorial activo (en formato simple)
    private steps: TourStep[] = [];
    // Permite quitar los escuchadores del paso actual
    private abort: AbortController | null = null;
    // Capa que desenfoca un poquito la zona oscura (el elemento resaltado queda nítido)
    private blurLayer: HTMLDivElement | null = null;
    private blurFrame = 0;
    // El seguimiento del blur corre fuera de Angular para no disparar detección de cambios en cada cuadro
    private zone = inject(NgZone);
    private language = inject(LanguageService);
    // Flujo pendiente: tutorial que continúa en otra página (null = ninguno)
    private flow: TourName | null = null;
    // true mientras se pasa de un paso a otro (puede haber un cambio de página en medio)
    private advancing = false;
    // Numera los arranques para ignorar uno que quedó esperando si empezó otro
    private runId = 0;

    constructor() {
        inject(Router)
            .events.pipe(filter((e): e is NavigationEnd => e instanceof NavigationEnd))
            .subscribe((e) => this.onNavigated(e.urlAfterRedirects));
    }

    // Inicia un tutorial desde el primer paso (cierra el anterior si había uno)
    start(name: TourName | 'admin', closeAdminProductForm?: () => void): void {
        this.flow = null;
        void this.run(this.stepsFor(name, closeAdminProductForm), closeAdminProductForm);
    }

    // Corre una lista de pasos. La usan start() y los servicios auxiliares.
    // Espera a que exista el primer elemento (la página puede estar cargando).
    async run(steps: TourStep[], onEnd?: () => void): Promise<void> {
        this.stop();

        this.steps = steps;
        if (steps.length === 0) return;

        const id = this.runId;
        const first = steps[0];
        const target = first.ready ?? first.element;
        if (target && !(await this.waitForElement(target))) {
            console.warn('Tutorial: no apareció el elemento', target);
            return;
        }
        // Mientras se esperaba empezó otro tutorial: este se descarta
        if (id !== this.runId) return;

        const reduceMotion = window.matchMedia('(prefers-reduced-motion: reduce)').matches;
        this.active = driver({
            // Animaciones: el recuadro se desliza de un elemento al siguiente
            animate: !reduceMotion,
            smoothScroll: !reduceMotion,
            overlayOpacity: 0.6,
            stagePadding: 8,
            stageRadius: 14,
            // Un clic en la zona oscura no cierra el tutorial por accidente
            overlayClickBehavior: () => { },
            // Sin teclado: las flechas permitirían saltarse los pasos que esperan un clic
            allowKeyboardControl: false,
            allowClose: true,
            // Clase propia para el estilo (ver styles.scss)
            popoverClass: 'terra-tour',
            showProgress: true,
            progressText: `{{current}} ${this.language.t('de')} {{total}}`,
            nextBtnText: this.language.t('Siguiente'),
            prevBtnText: this.language.t('Atrás'),
            doneBtnText: this.language.t('Listo'),
            closeBtnLabel: this.language.t('Cerrar tutorial'),
            steps: this.steps.map((step, index) => this.toDriveStep(step, index)),
            // Al cerrar (X o último paso) se limpia todo
            onDestroyed: () => {
                this.cleanup();
                this.stopBlur();
                this.active = null;
                onEnd?.();
            },
        });

        this.active.drive();
        this.startBlur();
    }

    // Cierra el tutorial activo, si hay uno
    stop(): void {
        this.runId++;
        this.advancing = false;
        this.active?.destroy();
        this.active = null;
        this.cleanup();
        this.stopBlur();
    }

    // ---- Flujo entre páginas (lo usan los servicios auxiliares) ----

    // Deja armado el siguiente tramo de un tutorial
    armFlow(name: TourName): void {
        this.flow = name;
    }

    // ¿Hay un tramo pendiente de ese tutorial?
    hasFlow(name: TourName): boolean {
        return this.flow === name;
    }

    // Tutorial que tiene un tramo pendiente (null = ninguno). Lo usa el login,
    // que es compartido por los tutoriales de pedido y de reserva.
    currentFlow(): TourName | null {
        return this.flow;
    }

    // Termina el flujo: ya no quedan tramos
    endFlow(): void {
        this.flow = null;
    }

    // Se ejecuta en cada cambio de página
    private onNavigated(url: string): void {
        const path = url.split('?')[0].split('#')[0];

        // El flujo pendiente se descarta si el cliente se va de sus páginas
        if (this.flow && !FLOW_ROUTES[this.flow].some((r) => path === r || path.startsWith(r + '/'))) {
            this.flow = null;
        }

        // Si el elemento resaltado desapareció sin que el tutorial lo provocara
        // (el cliente usó otro camino, p. ej. Enter en vez del botón), se cierra
        if (this.active) {
            const runId = this.runId;
            setTimeout(() => {
                if (runId !== this.runId || !this.active || this.advancing) return;
                const el = this.active.getActiveElement();
                if (el && !el.isConnected) this.stop();
            }, 300);
        }
    }

    // ¿El paso espera una acción del cliente (clic, escribir, que aparezca algo o que se vaya)?
    private waits(step: TourStep): boolean {
        return !!step.waitClick || !!step.waitInput || !!step.waitElement || !!step.waitNavigation;
    }

    // Convierte un paso simple en un paso de driver.js
    private toDriveStep(step: TourStep, index: number): DriveStep {
        const waits = this.waits(step);
        const buttons: NonNullable<Popover['showButtons']> = ['previous', 'close'];
        if (!waits || step.canSkip) buttons.push('next');

        return {
            // Se resuelve al momento de mostrar el paso: así, con un selector doble
            // ('#nav-cart, #nav-cart-mobile') se resalta el botón que se ve en esta pantalla
            element: step.element ? () => this.pickVisible(step.element!) : undefined,
            disableActiveInteraction: step.disableActiveInteraction,
            popover: {
                title: this.language.t(step.title),
                description: this.language.t(step.description),
                side: step.side,
                align: step.align,
                showButtons: buttons,
                nextBtnText: step.nextBtnText ? this.language.t(step.nextBtnText) : undefined,
                onPopoverRender: ({ previousButton }) => {
                    if (this.previousStep(index) === null) previousButton.style.display = 'none';
                },
                // "Siguiente" y "Atrás" pasan por el servicio (no por driver.js) para
                // esperar a que cargue la página y saltar los pasos opcionales que no existen
                onNextClick: () => void this.advance(index),
                onPrevClick: () => this.goBack(index),
                // La X cierra el tutorial y también descarta el flujo pendiente
                onCloseClick: () => {
                    this.flow = null;
                    this.stop();
                },
            },
            // Escucha desde el inicio de la transición para captar clics rápidos.
            onHighlightStarted: waits ? () => this.waitForAction(index) : undefined,
            // Al salir del paso se deja de escuchar
            onDeselected: () => this.cleanup(),
        };
    }

    // Espera un clic o que el cliente escriba, y entonces pasa al siguiente paso
    private waitForAction(index: number): void {
        this.cleanup();

        const step = this.steps[index];
        const abort = new AbortController();
        this.abort = abort;

        const done = (target: Element) => {
            abort.abort();
            step.afterClick?.(target);
            void this.advance(index);
        };

        // Fase de captura: se ve el evento aunque el componente lo procese después
        if (step.waitClick) {
            document.addEventListener(
                'click',
                (event) => {
                    const target = event.target as Element | null;
                    if (target?.closest(step.waitClick!)) done(target);
                },
                { capture: true, signal: abort.signal },
            );
        }

        // Avanza cuando aparece el elemento (se revisa cada 200 ms mientras el paso esté activo)
        if (step.waitElement) {
            const timer = setInterval(() => {
                const found = document.querySelector(step.waitElement!);
                if (found) {
                    clearInterval(timer);
                    done(found);
                }
            }, 200);
            abort.signal.addEventListener('abort', () => clearInterval(timer));
        }

        if (step.waitInput) {
            const { selector, valid } = step.waitInput;
            // "change" se dispara al salir del campo o pulsar Enter, no en cada tecla:
            // así el globo no se mueve mientras el cliente todavía está escribiendo.
            // También sirve para <select> y <input type="date">.
            document.addEventListener(
                'change',
                (event) => {
                    const input = event.target as HTMLInputElement | null;
                    if (!input?.matches(selector)) return;
                    if (!valid || valid(input.value)) done(input);
                },
                { capture: true, signal: abort.signal },
            );
        }
    }

    // Pasa al paso siguiente. Espera a que exista su elemento (cambio de página, datos
    // que cargan), salta los pasos condicionales que no aplican y los opcionales
    // cuyo elemento no aparece.
    private async advance(from: number): Promise<void> {
        if (this.advancing || !this.active) return;
        // Si mientras se espera arranca otro tramo (p. ej. la página siguiente retomó
        // el tutorial), este avance se descarta para no cerrar el tramo nuevo
        const id = this.runId;
        this.advancing = true;
        try {
            this.cleanup();
            this.steps[from].beforeNext?.();
            let next = from + 1;

            while (next < this.steps.length) {
                const step = this.steps[next];

                // Paso condicional: se deja que la página reaccione y se revisa
                if (step.when) {
                    await new Promise((r) => setTimeout(r, WHEN_DELAY_MS));
                    if (id !== this.runId || !this.active) return;
                    if (!step.when()) {
                        next++;
                        continue;
                    }
                }

                await step.beforeShow?.();
                if (id !== this.runId || !this.active) return;

                const target = step.ready ?? step.element;

                // Paso sin elemento (globo centrado): no hay nada que esperar
                if (!target) break;

                const wait = step.optional ? OPTIONAL_WAIT_MS : REQUIRED_WAIT_MS;
                const found = await this.waitForElement(target, wait);
                if (id !== this.runId || !this.active) return;
                if (found) break;

                if (!step.optional) {
                    console.warn('Tutorial: no apareció el elemento', target);
                    this.stop();
                    return;
                }
                // Opcional y no existe: se prueba con el siguiente
                next++;
            }

            // No quedan pasos: se termina el tutorial
            if (next >= this.steps.length) {
                this.active?.destroy();
                return;
            }

            this.active?.moveTo(next);
        } finally {
            if (id === this.runId) this.advancing = false;
        }
    }

    // Vuelve al paso anterior, saltando los opcionales y condicionales que no se mostraron
    private goBack(from: number): void {
        if (this.advancing) return;
        const previous = this.previousStep(from);
        if (previous !== null) this.active?.moveTo(previous);
    }

    private previousStep(from: number): number | null {
        let prev = from - 1;
        while (prev > 0 && this.isMissingOptional(prev)) prev--;
        // Las acciones pueden cambiar la pantalla o cerrar un modal.
        if (prev < 0 || this.waits(this.steps[prev]) || this.steps[prev].beforeNext) return null;
        return prev;
    }

    // true si el paso no se mostró: opcional sin elemento o condicional que no aplica
    private isMissingOptional(index: number): boolean {
        const step = this.steps[index];
        if (step.when && !step.when()) return true;
        return !!step.optional && !!step.element && !document.querySelector(step.element);
    }

    // Devuelve el primer elemento visible que cumple el selector. Sirve cuando hay una
    // versión de escritorio (oculta en móvil) y otra de móvil del mismo botón.
    private pickVisible(selector: string): Element {
        const all = Array.from(document.querySelectorAll(selector));
        return all.find((el) => el.getClientRects().length > 0) ?? all[0] ?? document.body;
    }

    // Espera (hasta timeoutMs) a que exista un elemento en la página
    private waitForElement(selector: string, timeoutMs = REQUIRED_WAIT_MS): Promise<boolean> {
        // Si ya está, se responde sin esperar el primer ciclo del temporizador
        if (document.querySelector(selector)) return Promise.resolve(true);

        return new Promise((resolve) => {
            const started = Date.now();
            const timer = setInterval(() => {
                if (document.querySelector(selector)) {
                    clearInterval(timer);
                    resolve(true);
                } else if (Date.now() - started > timeoutMs) {
                    clearInterval(timer);
                    resolve(false);
                }
            }, 100);
        });
    }

    // Crea la capa de blur y la mantiene recortada igual que la zona oscura de driver.js:
    // copia en cada cuadro el "recorte" del overlay (con el hueco del elemento resaltado),
    // así el hueco sigue nítido incluso mientras el recuadro se desliza.
    private startBlur(): void {
        this.stopBlur();

        const layer = document.createElement('div');
        layer.className = 'terra-blur';
        // Oculta hasta tener el primer recorte (si no, se desenfocaría también el hueco)
        layer.style.visibility = 'hidden';
        document.body.appendChild(layer);
        this.blurLayer = layer;

        let last = '';
        const tick = () => {
            const d = document
                .querySelector('.driver-overlay path')
                ?.getAttribute('d');
            if (d && d !== last) {
                last = d;
                // driver.js trae saltos de línea en el recorte y CSS no los acepta
                // dentro de path(): sin este paso el recorte se ignoraba y se
                // desenfocaba toda la pantalla, incluso el elemento resaltado.
                const clip = `path(evenodd, "${d.replace(/\s+/g, ' ').trim()}")`;
                if (CSS.supports('clip-path', clip)) {
                    layer.style.clipPath = clip;
                    layer.style.visibility = 'visible';
                } else {
                    // Si el navegador no entiende el recorte, mejor sin blur que con todo borroso
                    layer.style.visibility = 'hidden';
                }
            }
            this.blurFrame = requestAnimationFrame(tick);
        };
        this.zone.runOutsideAngular(tick);
    }

    // Quita la capa de blur
    private stopBlur(): void {
        cancelAnimationFrame(this.blurFrame);
        this.blurLayer?.remove();
        this.blurLayer = null;
    }

    // Quita los escuchadores del paso actual
    private cleanup(): void {
        this.abort?.abort();
        this.abort = null;
    }

    // Primer tramo de cada tutorial
    private stepsFor(name: TourName | 'admin', closeAdminProductForm?: () => void): TourStep[] {
        switch (name) {
            case 'order':
                return [
                    // 1) En el home: el cliente debe entrar a la carta
                    {
                        element: '#nav-menu',
                        title: 'Empieza por la carta',
                        description: 'Haz clic en "MENÚ" para ver todos nuestros platos.',
                        side: 'bottom',
                        align: 'center',
                        waitClick: '#nav-menu',
                    },
                    // 2) En la carta: categorías y filtros; no avanza hasta tocar uno
                    {
                        element: '#menu-filters',
                        ready: '#menu-filters .category-btn',
                        title: 'Filtra la carta',
                        description:
                            'Aquí están las categorías y, a la derecha, el botón de filtros por características. Toca cualquiera para continuar.',
                        side: 'bottom',
                        align: 'center',
                        waitClick: '#menu-filters button',
                        // Si tocó el botón de filtros, el panel quedó abierto: se cierra
                        afterClick: () => setTimeout(() => this.closeFeaturePanel(), 150),
                    },
                    // 3) En la carta: elegir un plato. Se resalta la primera tarjeta
                    //    (su id es "menu-card-<id>") y al tocarla se abre el detalle.
                    {
                        element: '[id^="menu-card-"]',
                        title: 'Elige un plato',
                        description:
                            'Toca este plato para ver su detalle. En tu pedido real podrás elegir el que quieras.',
                        side: 'right',
                        align: 'center',
                        waitClick: '[id^="menu-card-"]',
                    },
                    // 4) En el detalle: presentación (globo centrado). Espera a que cargue el producto.
                    {
                        ready: '#product-quantity',
                        title: 'El detalle del plato',
                        description:
                            'Aquí ves la descripción, las características y el precio. Ahora lo armamos a tu gusto.',
                    },
                    // 5) Adicionales. Solo existen si la categoría los tiene; si no, se salta.
                    //    No avanza al tocar: el cliente puede sumar varios antes de seguir.
                    {
                        element: '#product-addons',
                        optional: true,
                        title: 'Adicionales',
                        description:
                            'Si quieres, suma extras con + y −; puedes repetir el mismo. Cuando termines, pulsa "Siguiente".',
                        side: 'left',
                        align: 'center',
                    },
                    // 6) Cantidad (el total de la derecha se actualiza solo)
                    {
                        element: '#product-quantity',
                        title: 'Cantidad',
                        description:
                            'Elige cuántos platos quieres con + y −. El total se actualiza solo con los adicionales y la cantidad. Pulsa "Siguiente" cuando estés listo.',
                        side: 'top',
                        align: 'start',
                    },
                    // 7) Añadir a la orden: avanza cuando el cliente pulsa el botón
                    {
                        element: '#btn-add-to-order',
                        title: 'Añade el plato',
                        description:
                            'Revisa el total y pulsa "Añadir a la orden" para guardarlo en tu carrito.',
                        side: 'top',
                        align: 'center',
                        waitClick: '#btn-add-to-order',
                    },
                    // 8) Abrir el carrito (botón de escritorio o de móvil)
                    {
                        element: '#nav-cart, #nav-cart-mobile',
                        title: '¡Plato añadido!',
                        description:
                            'Ya está en tu carrito. Haz clic en el ícono del carrito para revisarlo.',
                        side: 'bottom',
                        align: 'end',
                        waitClick: '#nav-cart, #nav-cart-mobile',
                    },
                    // 9) Líneas del carrito. Se resalta la primera (su id es "cart-item-<id>").
                    {
                        element: '[id^="cart-item-"]',
                        title: 'Tu pedido',
                        description:
                            'Aquí ves cada plato con sus adicionales, el precio por unidad y la cantidad. Con la papelera puedes quitarlo.',
                        side: 'top',
                        align: 'center',
                    },
                    // 10) Total del pedido
                    {
                        element: '#cart-total',
                        title: 'Total del pedido',
                        description:
                            'Esta es la suma de todo lo que llevas. El domicilio se calcula más adelante.',
                        side: 'top',
                        align: 'center',
                    },
                    // 11) Confirmar: es el último paso de este tramo. Al pulsar, el tutorial
                    //     se cierra y deja el flujo armado: la página siguiente (login o
                    //     checkout) lo retoma con su servicio auxiliar.
                    {
                        element: '#btn-cart-checkout',
                        title: 'Confirma tu pedido',
                        description:
                            'Pulsa "Confirmar pedido" para continuar. Si no has iniciado sesión, te pediremos que lo hagas primero.',
                        side: 'top',
                        align: 'center',
                        waitClick: '#btn-cart-checkout',
                        afterClick: () => this.armFlow('order'),
                    },
                ];
            case 'reservation':
                return [
                    // En el home: entrar a reservas. Es el único paso de este tramo; al pulsar
                    // queda el flujo armado y la página de reservas lo retoma
                    // (TutorialReservationService), con o sin sesión.
                    {
                        element: '#nav-reservations',
                        title: 'Reserva tu mesa',
                        description:
                            'Haz clic en "RESERVAS" para ver el plano del restaurante y elegir tu mesa.',
                        side: 'bottom',
                        align: 'center',
                        waitClick: '#nav-reservations',
                        afterClick: () => this.armFlow('reservation'),
                    },
                ];
            case 'admin': {
                const dashboardControls = '#admin-dashboard-controls';
                const dashboardAvailable = () => document.querySelector(dashboardControls) !== null;
                const productsTab = '#tab-products';
                const addProduct = '#btn-add button';
                // En móvil y tablet la barra lateral es un panel oculto: se abre con el botón
                // de menú y se espera a que termine de deslizarse antes de resaltarla.
                // En escritorio el botón no se ve y no se hace nada.
                const openSidebar = async () => {
                    // Se deja que la página termine de reaccionar al clic anterior
                    // (al elegir una pestaña, el panel se cierra solo)
                    await new Promise((r) => setTimeout(r, WHEN_DELAY_MS));
                    const menu = document.querySelector<HTMLElement>('#btn-admin-menu');
                    const sidebar = document.querySelector('#admin-sidebar');
                    if (!menu || menu.getClientRects().length === 0) return;
                    if (sidebar?.classList.contains('open')) return;
                    menu.click();
                    await new Promise((r) => setTimeout(r, 350));
                };

                return [
                    {
                        element: 'app-admin-topbar h1',
                        ready: 'app-admin-dashboard',
                        title: 'Tu panel de administración',
                        description:
                            'En el Dashboard puedes seguir las ventas, la ganancia y los pedidos, comparar periodos y descargar reportes. Después veremos cómo gestionar el catálogo en Productos.',
                        side: 'bottom',
                        disableActiveInteraction: true,
                    },
                    {
                        element: dashboardControls,
                        title: 'Periodo y actualización',
                        description:
                            'El selector cambia el periodo de las métricas y gráficas que dependen de él. El indicador en vivo muestra la última actualización. Usa el botón de recarga si quieres actualizar en el momento.',
                        side: 'bottom',
                        optional: true,
                        disableActiveInteraction: true,
                    },
                    {
                        element: '#admin-dashboard-kpis',
                        title: 'Indicadores del día',
                        description:
                            'Consulta cuánto se ha vendido hoy, cuántos pedidos siguen pendientes, cuántas unidades se han vendido y cuánta ganancia se ha generado.',
                        side: 'bottom',
                        when: dashboardAvailable,
                        disableActiveInteraction: true,
                    },
                    {
                        element: '#admin-dashboard-sales',
                        title: 'Ventas, ganancia e ingresos',
                        description:
                            'Revisa cómo evolucionan las ventas y la ganancia por día, semana o mes. La comparación de ingresos muestra cuánto cambió el total frente al periodo anterior.',
                        when: dashboardAvailable,
                        disableActiveInteraction: true,
                    },
                    {
                        element: '#admin-dashboard-products',
                        title: 'Productos y categorías',
                        description:
                            'Identifica los seis productos más vendidos y revisa cómo se reparten las ventas entre categorías. Puedes ver ambas gráficas por unidades o por valor vendido.',
                        when: dashboardAvailable,
                        disableActiveInteraction: true,
                    },
                    {
                        element: '#admin-dashboard-orders',
                        title: 'Pedidos recientes y demorados',
                        description:
                            'Consulta los últimos pedidos e identifica cuáles llevan más tiempo esperando. Puedes ajustar el umbral de minutos para marcar una demora.',
                        when: dashboardAvailable,
                        disableActiveInteraction: true,
                    },
                    {
                        element: '#admin-dashboard-dishes',
                        title: 'Detalle de los platos',
                        description:
                            'Revisa el precio final, las unidades vendidas y la ganancia por plato en el periodo seleccionado.',
                        when: dashboardAvailable,
                        disableActiveInteraction: true,
                    },
                    {
                        element: '#btn-export',
                        title: 'Descarga un reporte',
                        description:
                            'Desde aquí puedes elegir un día, una semana o un rango de fechas y exportar a PDF, Excel o CSV. PDF y Excel también permiten incluir las gráficas.',
                        side: 'bottom',
                        when: dashboardAvailable,
                        disableActiveInteraction: true,
                    },
                    {
                        element: productsTab,
                        title: 'Ve a Productos',
                        description: 'Haz clic en "Productos" en la barra lateral para revisar el catálogo.',
                        side: 'right',
                        waitClick: productsTab,
                        beforeShow: openSidebar,
                    },
                    {
                        element: '#admin-product-filters',
                        title: 'Encuentra un producto',
                        description:
                            'Usa el buscador de arriba y los filtros de categoría, estado y características para acotar la lista del catálogo.',
                        side: 'bottom',
                        disableActiveInteraction: true,
                    },
                    {
                        element:
                            '#admin-product-table tbody tr[app-admin-product-row], ' +
                            '#admin-product-table:not(:has(tbody tr[app-admin-product-row])) thead',
                        title: 'Acciones rápidas de cada fila',
                        description:
                            'Cuando hay productos en la lista, puedes tocar "Activo" o "Inactivo" para cambiar su disponibilidad. El lápiz abre la edición y la papelera abre la confirmación de eliminación.',
                        side: 'bottom',
                        disableActiveInteraction: true,
                    },
                    {
                        element: addProduct,
                        title: 'Añade un producto',
                        description: 'Haz clic en "Añadir producto" para abrir el formulario de creación.',
                        side: 'bottom',
                        waitClick: addProduct,
                    },
                    {
                        element: '#admin-product-form',
                        title: 'Formulario de creación',
                        description:
                            'Aquí defines el nombre, precio, categoría, descripción y URL de imagen. También puedes marcar las características y elegir si el producto quedará activo. Al terminar, "Añadir producto" guarda el registro.',
                        side: 'right',
                        disableActiveInteraction: true,
                        nextBtnText: 'Cerrar formulario',
                        beforeNext: closeAdminProductForm,
                    },
                    {
                        element: '#admin-sidebar',
                        ready: 'app-admin:not(:has(#admin-product-form)) #admin-sidebar',
                        beforeShow: openSidebar,
                        title: 'Explora el resto del panel',
                        description:
                            'Desde esta barra puedes volver al Dashboard, gestionar productos, categorías, adicionales, cupones y anuncios, revisar reseñas y órdenes, y administrar usuarios, operadores y domiciliarios.',
                        side: 'right',
                        disableActiveInteraction: true,
                    },
                ];
            }
            default:
                return [];
        }
    }

    // Cierra el panel de filtros por características si quedó abierto
    private closeFeaturePanel(): void {
        const button = document.querySelector<HTMLElement>(
            '#menu-filters .feature-filter-btn',
        );
        if (button?.getAttribute('aria-expanded') === 'true') {
            button.click();
        }
    }
}
