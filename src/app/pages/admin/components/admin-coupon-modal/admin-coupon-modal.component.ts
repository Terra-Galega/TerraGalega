import {
    Component,
    EventEmitter,
    Input,
    Output,
    SimpleChanges,
    inject,
} from '@angular/core';
import { DialogDirective } from '../../../../directives/dialog.directive';
import { Client } from '../../../../models/client';
import { Coupon, CouponPayload, DiscountType } from '../../../../models/coupon';
import { ClientService } from '../../../../services/client.service';
import { CouponService } from '../../../../services/coupon.service';
import { ButtonComponent } from '../../../../components/button/button.component';
import { IconButtonComponent } from '../../../../components/icon-button/icon-button.component';
import { FormFieldComponent } from '../../../../components/form-field/form-field.component';
import { CheckOptionComponent } from '../../../../components/check-option/check-option.component';
import { ToastService } from '../../../../services/toast.service';
import { getErrorMessage } from '../../../../utils/http-error';

/**
 * Ventana flotante para crear o editar un cupón.
 * Si recibe un cupón, lo edita; si recibe null, crea uno nuevo.
 * El cupón es para todos los clientes (casilla marcada) o para un cliente
 * específico, que se busca por nombre, apellido o correo. Valida el formulario
 * y llama al servicio correspondiente. Avisa al padre con "saved" y "closed".
 */

const CODE_PATTERN = /^[A-Za-z0-9_-]{3,40}$/;
const DESCRIPTION_MAX = 255;
const MAX_AMOUNT = 10_000_000;
const MAX_USES = 1_000_000;

@Component({
    selector: 'app-admin-coupon-modal',
    imports: [DialogDirective, 
        ButtonComponent,
        IconButtonComponent,
        FormFieldComponent,
        CheckOptionComponent,
    ],
    templateUrl: './admin-coupon-modal.component.html',
})
export class AdminCouponModalComponent {
    private couponService = inject(CouponService);
    private clientService = inject(ClientService);
    private toast = inject(ToastService);

    // Cupón a editar; null significa "crear uno nuevo"
    @Input() coupon: Coupon | null = null;

    // closed: se debe cerrar la ventana. saved: se guardó con éxito.
    @Output() closed = new EventEmitter<void>();
    @Output() saved = new EventEmitter<void>();

    // Clientes entre los que se puede buscar al dueño del cupón
    clients: Client[] = [];

    // Campos del formulario
    code = '';
    description = '';
    discountType: DiscountType = 'PERCENTAGE';
    discountValue: number | null = null;
    // Opcionales: null = sin tope / sin mínimo / sin límite de usos
    maxDiscount: number | null = null;
    minOrderAmount: number | null = null;
    maxUses: number | null = null;
    // Fechas en el formato del campo "datetime-local" (AAAA-MM-DDTHH:mm);
    // vacío = sin fecha
    startsAt = '';
    expiresAt = '';
    // true = cupón general (para todos); false = para un cliente específico
    forEveryone = true;
    // Texto que se escribe para buscar al cliente
    clientSearch = '';
    // Cliente elegido como dueño (solo cuando forEveryone es false)
    selectedClient: Pick<Client, 'id' | 'name' | 'lastName'> | null = null;
    // Si está activo, se puede usar en los pedidos
    active = true;

    // Mensaje de error que se muestra dentro del formulario
    errorMessage = '';

    readonly descriptionMax = DESCRIPTION_MAX;
    readonly maxAmount = MAX_AMOUNT;
    readonly maxUsesLimit = MAX_USES;       
    saving = false;

    constructor() {
        // Se cargan los clientes una sola vez para poder buscarlos
        this.clientService.getClients().subscribe({
            next: (clients) => {
                this.clients = clients;
            },
            error: (error) => {
                console.error('Error cargando clientes:', error);
            },
        });
    }

    // Se ejecuta cada vez que el padre cambia el @Input "coupon":
    // llena el formulario con el cupón (editar) o lo deja vacío (crear)
    ngOnChanges(changes: SimpleChanges) {
        if (!changes['coupon']) {
            return;
        }

        this.errorMessage = '';
        this.clientSearch = '';

        this.saving = false;

        if (this.coupon) {
            this.code = this.coupon.code;
            this.description = this.coupon.description ?? '';
            this.discountType = this.coupon.discountType;
            this.discountValue = this.coupon.discountValue;
            this.maxDiscount = this.coupon.maxDiscount;
            this.minOrderAmount = this.coupon.minOrderAmount;
            this.maxUses = this.coupon.maxUses;
            // El campo de fecha solo entiende hasta los minutos
            this.startsAt = this.coupon.startsAt
                ? this.coupon.startsAt.slice(0, 16)
                : '';
            this.expiresAt = this.coupon.expiresAt
                ? this.coupon.expiresAt.slice(0, 16)
                : '';
            this.forEveryone = !this.coupon.client;
            this.selectedClient = this.coupon.client;
            this.active = this.coupon.active;
        } else {
            this.code = '';
            this.description = '';
            this.discountType = 'PERCENTAGE';
            this.discountValue = null;
            this.maxDiscount = null;
            this.minOrderAmount = null;
            this.maxUses = null;
            this.startsAt = '';
            this.expiresAt = '';
            this.forEveryone = true;
            this.selectedClient = null;
            this.active = true;
        }
    }

    // Los siguientes "set..." guardan en el componente lo que el usuario escribe
    setCode(event: Event) {
        this.code = (event.target as HTMLInputElement).value;
    }

    setDescription(event: Event) {
        this.description = (event.target as HTMLTextAreaElement).value;
    }

    setDiscountType(event: Event) {
        this.discountType = (event.target as HTMLSelectElement)
            .value as DiscountType;
    }

    setDiscountValue(event: Event) {
        this.discountValue = this.toNumber(event);
    }

    setMaxDiscount(event: Event) {
        this.maxDiscount = this.toNumber(event);
    }

    setMinOrderAmount(event: Event) {
        this.minOrderAmount = this.toNumber(event);
    }

    setMaxUses(event: Event) {
        this.maxUses = this.toNumber(event);
    }

    setStartsAt(event: Event) {
        this.startsAt = (event.target as HTMLInputElement).value;
    }

    setExpiresAt(event: Event) {
        this.expiresAt = (event.target as HTMLInputElement).value;
    }

    // Casilla "Para todos los clientes". Al marcarla se descarta el cliente elegido.
    setForEveryone(checked: boolean) {
        this.forEveryone = checked;
        if (checked) {
            this.selectedClient = null;
            this.clientSearch = '';
        }
    }

    // Texto del buscador de clientes
    setClientSearch(event: Event) {
        this.clientSearch = (event.target as HTMLInputElement).value;
    }

    // Clientes que coinciden con lo escrito (nombre, apellido o correo), máx. 6
    get filteredClients(): Client[] {
        const query = this.normalize(this.clientSearch);
        if (!query) {
            return [];
        }
        return this.clients
            .filter((client) =>
                this.normalize(
                    `${client.name} ${client.lastName} ${client.email}`,
                ).includes(query),
            )
            .slice(0, 6);
    }

    // Elige un cliente de los resultados de la búsqueda
    selectClient(client: Client) {
        this.selectedClient = client;
        this.clientSearch = '';
    }

    // Quita el cliente elegido para buscar otro
    clearClient() {
        this.selectedClient = null;
    }

    // Minúsculas y sin tildes, para que "maria" encuentre "María"
    private normalize(text: string): string {
        return text
            .toLowerCase()
            .normalize('NFD')
            .replace(/[\u0300-\u036f]/g, '');
    }

    // Marca o desmarca "Activo" (sección "Estado")
    toggleActive() {
        this.active = !this.active;
    }

    // Convierte lo escrito en un campo numérico: vacío se vuelve null
    private toNumber(event: Event): number | null {
        const value = (event.target as HTMLInputElement).value;
        return value.trim() === '' ? null : Number(value);
    }

        // true si es un número entero finito
    private isWholeNumber(value: number | null): value is number {
        return value !== null && Number.isFinite(value) && Number.isInteger(value);
    }

    // Revisa los campos. Devuelve el mensaje del primer error,
    // o un texto vacío si todo está bien
    private validate(): string {
        const code = this.code.trim();

        // Código
        if (!code) return 'El código del cupón es obligatorio.';
        if (!CODE_PATTERN.test(code))
            return 'El código debe tener entre 3 y 40 caracteres y solo puede incluir letras, números, guion (-) y guion bajo (_), sin espacios.';

        // Valor del descuento
        const value = this.discountValue;
        if (value === null || !Number.isFinite(value) || value <= 0)
            return 'El valor del descuento es obligatorio y debe ser mayor que cero.';
        if (this.discountType === 'PERCENTAGE' && value > 100)
            return 'El porcentaje no puede ser mayor que 100.';
        if (this.discountType === 'FIXED_AMOUNT') {
            if (!Number.isInteger(value))
                return 'El monto fijo debe ser un número entero de pesos.';
            if (value > MAX_AMOUNT)
                return `El monto fijo no puede superar $${MAX_AMOUNT.toLocaleString('es-CO')}.`;
        }

        // Tope: solo existe (y solo se valida) en los cupones de porcentaje
        if (this.discountType === 'PERCENTAGE' && this.maxDiscount !== null) {
            if (!this.isWholeNumber(this.maxDiscount) || this.maxDiscount <= 0)
                return 'El tope del descuento debe ser un número entero mayor que cero.';
            if (this.maxDiscount > MAX_AMOUNT)
                return `El tope del descuento no puede superar $${MAX_AMOUNT.toLocaleString('es-CO')}.`;
        }

        // Subtotal mínimo
        if (this.minOrderAmount !== null) {
            if (!this.isWholeNumber(this.minOrderAmount) || this.minOrderAmount < 0)
                return 'El subtotal mínimo debe ser un número entero de cero o más.';
            if (this.minOrderAmount > MAX_AMOUNT)
                return `El subtotal mínimo no puede superar $${MAX_AMOUNT.toLocaleString('es-CO')}.`;
        }

        // Usos máximos
        if (this.maxUses !== null) {
            if (!this.isWholeNumber(this.maxUses) || this.maxUses < 1)
                return 'Los usos máximos deben ser un número entero de al menos 1.';
            if (this.maxUses > MAX_USES)
                return `Los usos máximos no pueden superar ${MAX_USES.toLocaleString('es-CO')}.`;
        }

        // Fechas
        const starts = this.startsAt ? new Date(this.startsAt) : null;
        const expires = this.expiresAt ? new Date(this.expiresAt) : null;

        if (starts && isNaN(starts.getTime())) return 'La fecha de inicio no es válida.';
        if (expires && isNaN(expires.getTime()))
            return 'La fecha de vencimiento no es válida.';
        if (starts && expires && expires <= starts)
            return 'El vencimiento debe ser posterior al inicio.';

        // Un vencimiento en el pasado solo se rechaza si es nuevo o cambió; así
        // se puede seguir editando (o desactivando) un cupón que ya venció
        const originalExpires = this.coupon?.expiresAt?.slice(0, 16) ?? '';
        if (expires && this.expiresAt !== originalExpires && expires <= new Date())
            return 'El vencimiento debe ser una fecha futura.';

        // Dueño
        if (!this.forEveryone && !this.selectedClient)
            return 'Busca y elige un cliente, o marca "Para todos los clientes".';

        return '';
    }

    // Botón "Guardar cambios" / "Añadir cupón"
    save() {
        if (this.saving) {
            return;
        }

        // Si hay un error de validación, se muestra y no se envía nada
        this.errorMessage = this.validate();

        if (this.errorMessage) {
            return;
        }

        // Lo que se envía al back. El tope solo aplica a los porcentajes.
        // Del dueño solo se manda el id (null = cupón general).
        const payload: CouponPayload = {
            code: this.code.trim().toUpperCase(),
            description: this.description.trim() || null,
            discountType: this.discountType,
            discountValue: this.discountValue!,
            maxDiscount: this.discountType === 'PERCENTAGE' ? this.maxDiscount : null,
            minOrderAmount: this.minOrderAmount,
            startsAt: this.startsAt || null,
            expiresAt: this.expiresAt || null,
            maxUses: this.maxUses,
            active: this.active,
            client:
                !this.forEveryone && this.selectedClient
                    ? { id: this.selectedClient.id }
                    : null,
        };

        this.saving = true;

        // EDITAR
        if (this.coupon) {
            this.couponService.updateCoupon(this.coupon.id, payload).subscribe({
                next: () =>
                    this.onSuccess('Cambios del cupón guardados correctamente'),
                error: (error) => this.onError(error),
            });

            return;
        }

        // CREAR (no se manda id, lo genera el backend)
        this.couponService.createCoupon(payload).subscribe({
            next: () => this.onSuccess('Cupón creado correctamente'),
            error: (error) => this.onError(error),
        });
    }

    // Error al guardar: se muestra dentro del formulario y la ventana sigue abierta
    private onError(error: any) {
        console.error('Error guardando cupón:', error);
        this.saving = false;
        this.errorMessage = getErrorMessage(error, 'No se pudo guardar el cupón.');
    }

    // Guardado exitoso: refresca la tabla, avisa al usuario y cierra la ventana
    private onSuccess(message: string) {
        this.couponService.refresh();
        this.toast.show(message);
        this.saved.emit();
        this.closed.emit();
    }


    // Botón "Cancelar" y la X: pide al padre cerrar la ventana
    close() {
        this.closed.emit();
    }
}