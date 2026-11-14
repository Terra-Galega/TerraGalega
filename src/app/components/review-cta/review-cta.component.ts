import { Component, Input } from '@angular/core';
import { Router } from '@angular/router';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { ButtonComponent } from '../button/button.component';
import { ReviewModalComponent } from '../review-modal/review-modal.component';
import { AuthService } from '../../services/auth.service';
import { ReviewService } from '../../services/review.service';
import { Review } from '../../models/review';

/**
 * Botón para escribir (o editar) la reseña propia, junto con su ventana.
 * Lo usan el home y Nosotros. Sin sesión lleva a crear una cuenta y, al
 * entrar, devuelve a la página desde la que se pulsó.
 */
@Component({
    selector: 'app-review-cta',
    imports: [ButtonComponent, ReviewModalComponent],
    templateUrl: './review-cta.component.html',
    host: { class: 'block text-center' },
})
export class ReviewCtaComponent {
    // Página a la que se vuelve después de iniciar sesión
    @Input() redirect: 'home' | 'aboutUs' = 'home';

    // true si hay una sesión iniciada
    loggedIn = false;
    // true si la sesión es de un cliente (el único rol que puede opinar)
    isClient = false;
    // Reseña del cliente con sesión; null si todavía no ha escrito una
    myReview: Review | null = null;
    // true mientras la ventana de la reseña está abierta
    modalOpen = false;

    constructor(
        private auth: AuthService,
        private router: Router,
        private reviewService: ReviewService,
    ) {
        // Se actualiza cuando cambia la sesión (inicio o cierre de sesión)
        this.auth.session$.pipe(takeUntilDestroyed()).subscribe((session) => {
            this.loggedIn = session !== null;
            this.isClient = session?.type === 'client';
            this.loadMyReview();
        });

        // Y cuando el cliente guarda o elimina su reseña
        this.reviewService.refresh$
            .pipe(takeUntilDestroyed())
            .subscribe(() => this.loadMyReview());
    }

    // Texto del botón según el cliente ya tenga reseña o no
    get buttonLabel(): string {
        return this.myReview ? 'Editar mi reseña' : 'Escribir una reseña';
    }

    // Trae la reseña del cliente con sesión (los demás roles no tienen)
    private loadMyReview() {
        if (!this.isClient) {
            this.myReview = null;
            return;
        }
        this.reviewService.getMyReview().subscribe({
            next: (review) => (this.myReview = review),
            error: () => (this.myReview = null),
        });
    }

    // Sin sesión lleva a crear una cuenta (y de vuelta aquí al entrar);
    // con sesión abre la ventana
    onWrite() {
        if (!this.loggedIn) {
            this.router.navigate(['/login'], {
                queryParams: { tab: 'signup', redirect: this.redirect },
            });
            return;
        }
        this.modalOpen = true;
    }

    closeModal() {
        this.modalOpen = false;
    }
}