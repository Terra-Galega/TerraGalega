import { TranslatePipe } from '../../services/translate.pipe';
import { DatePipe } from '@angular/common';
import { Component, Input, inject } from '@angular/core';
import { ClientCoupon, isCouponActive } from '../../models/coupon';
import { ToastService } from '../../services/toast.service';

// Tarjeta de un cupón del cliente: código, descuento, condiciones y vigencia.
// Si ya no se puede usar, se ve atenuada y con una etiqueta (Usado, Vencido, Agotado).
@Component({
    selector: 'app-coupon-card',
    imports: [DatePipe, TranslatePipe],
    templateUrl: './coupon-card.component.html',
})
export class CouponCardComponent {
    private toast = inject(ToastService);

    @Input({ required: true }) coupon!: ClientCoupon;

    get isActive(): boolean {
        return isCouponActive(this.coupon);
    }

    // Texto grande de la franja: "10%" o "$5.000"
    get valueText(): string {
        return this.coupon.discountType === 'PERCENTAGE'
            ? `${this.coupon.discountValue}%`
            : `$${this.coupon.discountValue.toLocaleString('es-CO')}`;
    }

    // Tamaño del número según su largo, para que siempre quepa en la franja
    // (la franja mide lo mismo en todas las tarjetas)
    get valueSize(): string {
        const length = this.valueText.length;

        if (length <= 4) {
            return 'text-3xl';
        }
        if (length <= 7) {
            return 'text-xl';
        }
        return 'text-lg';
    }

    // Condiciones del cupón en una sola línea, separadas por puntos
    get conditions(): string {
        const parts: string[] = [];

        if (this.coupon.minOrderAmount) {
            parts.push(
                `Mínimo $${this.coupon.minOrderAmount.toLocaleString('es-CO')}`,
            );
        }
        if (this.coupon.discountType === 'PERCENTAGE' && this.coupon.maxDiscount) {
            parts.push(
                `Tope $${this.coupon.maxDiscount.toLocaleString('es-CO')}`,
            );
        }
        parts.push('1 uso por cliente');

        return parts.join(' · ');
    }

    // Texto de la etiqueta cuando el cupón ya no se puede usar
    get statusLabel(): string {
        switch (this.coupon.status) {
            case 'USED':
                return 'Usado';
            case 'EXHAUSTED':
                return 'Agotado';
            default:
                return 'Vencido';
        }
    }

    // Copia el código para pegarlo luego en el checkout
    copyCode(): void {
        navigator.clipboard
            ?.writeText(this.coupon.code)
            .then(() => this.toast.show('Código copiado', this.coupon.code))
            .catch(() => { });
    }
}