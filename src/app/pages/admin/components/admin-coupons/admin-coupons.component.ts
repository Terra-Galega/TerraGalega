import { Component, EventEmitter, Input, Output, inject } from '@angular/core';
import { DatePipe, DecimalPipe } from '@angular/common';
import { EmptyStateComponent } from '../../../../components/empty-state/empty-state.component';
import { IconButtonComponent } from '../../../../components/icon-button/icon-button.component';
import { ModalComponent } from '../../../../components/modal/modal.component';
import { Coupon } from '../../../../models/coupon';
import { CouponService } from '../../../../services/coupon.service';
import { ToastService } from '../../../../services/toast.service';
import { getErrorMessage } from '../../../../utils/http-error';

/**
 * Tabla de cupones del panel de administración.
 * Muestra cada cupón con su descuento, vigencia, dueño y estado; permite
 * filtrar por código, descripción o cliente, activar/desactivar, editar y
 * eliminar (con ventana de confirmación y ventana de error si el servidor
 * rechaza la eliminación).
 */
@Component({
    selector: 'app-admin-coupons',
    imports: [
        DatePipe,
        DecimalPipe,
        EmptyStateComponent,
        IconButtonComponent,
        ModalComponent,
    ],
    templateUrl: './admin-coupons.component.html',
})
export class AdminCouponsComponent {
    private couponService = inject(CouponService);
    private toast = inject(ToastService);

    // Texto de búsqueda que escribe el admin en la barra superior
    @Input() searchTerm = '';

    // Avisa al padre qué cupón se quiere editar (el padre abre el modal)
    @Output() editCoupon = new EventEmitter<Coupon>();

    // Encabezados de la tabla
    readonly columns = [
        'Cupón',
        'Descuento',
        'Vigencia',
        'Dueño',
        'Estado',
        'Acciones',
    ];

    // Todos los cupones que devuelve el servidor
    coupons: Coupon[] = [];
    // Cupones que coinciden con la búsqueda (los que se dibujan)
    filteredCoupons: Coupon[] = [];

    // Controlan la ventana de confirmación y la ventana de error
    showDeleteModal = false;
    showErrorModal = false;

    // Id del cupón que se está a punto de eliminar
    selectedCouponId: number | null = null;

    // Textos de la ventana de confirmación
    confirmTitle = 'Eliminar cupón';
    confirmMessage = '¿Estás seguro de que deseas eliminar este cupón?';

    // Textos de la ventana de error (se llenan según el fallo)
    errorTitle = '';
    errorMessage = '';

    constructor() {
        this.loadCoupons();

        // Cada vez que otro componente pide refrescar (por ejemplo, tras guardar
        // en el modal), se vuelve a cargar la lista
        this.couponService.refresh$.subscribe(() => {
            this.loadCoupons();
        });
    }

    // Pide los cupones al servidor y vuelve a aplicar el filtro
    loadCoupons() {
        this.couponService.getCoupons().subscribe({
            next: (coupons) => {
                this.coupons = coupons;
                this.filterCoupons();
            },
            error: (error) => {
                console.error('Error cargando cupones:', error);
            },
        });
    }

    // Angular lo llama cuando cambia un @Input (aquí, searchTerm)
    ngOnChanges() {
        this.filterCoupons();
    }

    // Deja solo los cupones cuyo código, descripción o cliente contienen el
    // texto buscado (sin distinguir mayúsculas de minúsculas)
    filterCoupons() {
        const value = this.searchTerm.trim().toLowerCase();

        this.filteredCoupons = this.coupons.filter((coupon) => {
            const clientName = coupon.client
                ? `${coupon.client.name} ${coupon.client.lastName}`
                : '';

            return (
                coupon.code.toLowerCase().includes(value) ||
                (coupon.description ?? '').toLowerCase().includes(value) ||
                clientName.toLowerCase().includes(value)
            );
        });
    }

    // true si el cupón tiene fecha de vencimiento y ya pasó
    isExpired(coupon: Coupon): boolean {
        return !!coupon.expiresAt && new Date(coupon.expiresAt) < new Date();
    }

    // Texto con las condiciones del cupón (tope, mínimo y usos)
    conditions(coupon: Coupon): string {
        const parts: string[] = [];

        if (coupon.maxDiscount) {
            parts.push(`Tope $${coupon.maxDiscount.toLocaleString('es-CO')}`);
        }
        if (coupon.minOrderAmount) {
            parts.push(`Mín. $${coupon.minOrderAmount.toLocaleString('es-CO')}`);
        }
        if (coupon.maxUses) {
            parts.push(`${coupon.maxUses} ${coupon.maxUses === 1 ? 'uso' : 'usos'}`);
        }

        return parts.length > 0 ? parts.join(' · ') : 'Sin condiciones';
    }

    // Activa o desactiva el cupón (botón "Activo/Inactivo" de la fila)
    toggleCoupon(coupon: Coupon) {
        this.couponService.toggleCouponActiveStatus(coupon.id).subscribe({
            next: () => {
                // El estado que llega aún es el anterior al cambio
                this.toast.show(coupon.active ? 'Cupón desactivado' : 'Cupón activado');
                // Se recarga la lista para ver el nuevo estado
                this.couponService.refresh();
            },
            error: (error) => {
                console.error('Error cambiando estado del cupón:', error);
            },
        });
    }

    // Reenvía al padre el cupón que se quiere editar
    openEdit(coupon: Coupon) {
        this.editCoupon.emit(coupon);
    }

    // Paso 1 de eliminar: guarda cuál es el cupón y abre la confirmación
    removeCoupon(coupon: Coupon) {
        this.selectedCouponId = coupon.id;
        this.showDeleteModal = true;
    }

    // El admin se arrepintió: se cierra la confirmación y se olvida el cupón
    cancelDelete() {
        this.showDeleteModal = false;
        this.selectedCouponId = null;
    }

    // Paso 2 de eliminar: el admin confirmó
    confirmDelete() {
        if (this.selectedCouponId === null) {
            return;
        }

        // Se guarda el id en una variable local porque se limpia justo después
        const id = this.selectedCouponId;

        this.showDeleteModal = false;
        this.selectedCouponId = null;

        this.couponService.deleteCoupon(id).subscribe({
            next: () => {
                this.couponService.refresh();
                this.toast.show('Cupón eliminado correctamente');
            },
            error: (error) => {
                console.error('Error eliminando cupón:', error);
                // Se muestra el mensaje que mandó el servidor, o uno genérico
                this.openErrorModal(
                    'No se pudo eliminar el cupón',
                    getErrorMessage(
                        error,
                        'Ocurrió un error al eliminar el cupón. Por favor, inténtalo de nuevo.',
                    ),
                );
            },
        });
    }

    // Abre la ventana de error con el título y mensaje indicados
    private openErrorModal(title: string, message: string) {
        this.errorTitle = title;
        this.errorMessage = message;
        this.showErrorModal = true;
    }

    // Cierra la ventana de error y limpia sus textos
    closeErrorModal() {
        this.showErrorModal = false;
        this.errorTitle = '';
        this.errorMessage = '';
    }
}