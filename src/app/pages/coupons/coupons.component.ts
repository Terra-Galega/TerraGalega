import { TranslatePipe } from '../../services/translate.pipe';
import { Component, OnInit, inject } from '@angular/core';
import { Router } from '@angular/router';
import { NavbarComponent } from '../../components/navbar/navbar.component';
import { FooterComponent } from '../../components/footer/footer.component';
import { PageHeaderComponent } from '../../components/page-header/page-header.component';
import { EmptyStateComponent } from '../../components/empty-state/empty-state.component';
import { CouponCardComponent } from '../../components/coupon-card/coupon-card.component';
import { AuthService } from '../../services/auth.service';
import { CouponService } from '../../services/coupon.service';
import { ClientCoupon, isCouponActive } from '../../models/coupon';
import { getErrorMessage } from '../../utils/http-error';

// Página "Mis cupones": los cupones personales del cliente, separados en
// Activos (se pueden usar ahora) y Expirados (vencidos, usados o agotados).
@Component({
    selector: 'app-coupons',
    imports: [
        NavbarComponent,
        FooterComponent,
        PageHeaderComponent,
        EmptyStateComponent,
        CouponCardComponent,
    TranslatePipe],
    templateUrl: './coupons.component.html',
})
export class CouponsComponent implements OnInit {
    private auth = inject(AuthService);
    private couponService = inject(CouponService);
    private router = inject(Router);

    // true hasta recibir la respuesta del servidor
    loading = true;
    coupons: ClientCoupon[] = [];

    ngOnInit(): void {
        // El guard ya exige sesión de cliente; esto es solo por seguridad
        const session = this.auth.session;
        if (!session || session.type !== 'client') {
            this.router.navigate(['/login']);
            return;
        }

        this.couponService.getClientCoupons().subscribe({
            next: (coupons) => {
                this.coupons = coupons;
                this.loading = false;
            },
            error: (error) => {
                console.error('Error cargando cupones:', error);
                this.loading = false;
                this.router.navigate(['/error'], {
                    replaceUrl: true,
                    queryParams: {
                        status: error.status,
                        message: getErrorMessage(error, 'No se pudieron cargar tus cupones.'),
                    },
                });
            },
        });
    }

    // Los que vencen primero van arriba (los que no vencen, al final)
    get activeCoupons(): ClientCoupon[] {
        return this.coupons
            .filter(isCouponActive)
            .sort((a, b) =>
                (a.expiresAt ?? '9999').localeCompare(b.expiresAt ?? '9999'),
            );
    }

    // Los más recientes primero
    get expiredCoupons(): ClientCoupon[] {
        return this.coupons
            .filter((coupon) => !isCouponActive(coupon))
            .sort((a, b) => (b.expiresAt ?? '').localeCompare(a.expiresAt ?? ''));
    }

    goToMenu(): void {
        this.router.navigate(['/menu']);
    }
}