import { Component, Input, OnChanges, inject } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { EmptyStateComponent } from '../../../../components/empty-state/empty-state.component';
import { IconButtonComponent } from '../../../../components/icon-button/icon-button.component';
import { ModalComponent } from '../../../../components/modal/modal.component';
import { StarIconComponent } from '../../../../components/star-icon/star-icon.component';
import { Review } from '../../../../models/review';
import { ReviewService } from '../../../../services/review.service';
import { ToastService } from '../../../../services/toast.service';
import { getErrorMessage } from '../../../../utils/http-error';

/**
 * Tabla de reseñas en el panel de administración. Muestra todas (visibles y
 * ocultas), permite filtrar por autor o comentario, ocultar o volver a mostrar
 * una reseña y eliminarla.
 */
@Component({
    selector: 'app-admin-reviews',
    imports: [
        EmptyStateComponent,
        IconButtonComponent,
        ModalComponent,
        StarIconComponent,
    ],
    templateUrl: './admin-reviews.component.html',
})
export class AdminReviewsComponent implements OnChanges {
    private reviewService = inject(ReviewService);
    private toast = inject(ToastService);

    // Texto de búsqueda que escribe el admin en la barra superior
    @Input() searchTerm = '';

    // Encabezados de la tabla
    readonly columns = [
        'Autor',
        'Calificación',
        'Comentario',
        'Fecha',
        'Estado',
        'Acciones',
    ];

    // Las cinco posiciones de estrella; las que superan la nota se atenúan
    readonly stars = [1, 2, 3, 4, 5];

    // Todas las reseñas y las que coinciden con la búsqueda
    reviews: Review[] = [];
    filteredReviews: Review[] = [];

    // Controlan la ventana de confirmación y la ventana de error
    showDeleteModal = false;
    showErrorModal = false;

    // Id de la reseña que se está a punto de eliminar
    selectedReviewId: number | null = null;

    // Textos de la ventana de confirmación
    confirmTitle = 'Eliminar reseña';
    confirmMessage =
        '¿Estás seguro de que deseas eliminar esta reseña? El cliente podrá escribir otra.';

    // Textos de la ventana de error
    errorTitle = '';
    errorMessage = '';

    constructor() {
        this.loadReviews();

        // Cada vez que se oculta, muestra o elimina una reseña se vuelve a cargar la lista
        this.reviewService.refresh$
            .pipe(takeUntilDestroyed())
            .subscribe(() => this.loadReviews());
    }

    // Cuántas reseñas están ocultas (para el contador)
    get hiddenCount(): number {
        return this.reviews.filter((review) => !review.visible).length;
    }

    // Pide todas las reseñas al servidor y vuelve a aplicar el filtro
    loadReviews() {
        this.reviewService.getAllReviews().subscribe({
            next: (reviews) => {
                this.reviews = reviews;
                this.filterReviews();
            },
            error: (error) => {
                console.error('Error cargando reseñas:', error);
            },
        });
    }

    // Angular lo llama cuando cambia un @Input (aquí, searchTerm)
    ngOnChanges() {
        this.filterReviews();
    }

    // Deja solo las reseñas cuyo autor o comentario contiene el texto buscado
    filterReviews() {
        const value = this.searchTerm.trim().toLowerCase();

        this.filteredReviews = this.reviews.filter(
            (review) =>
                review.authorName.toLowerCase().includes(value) ||
                review.comment.toLowerCase().includes(value),
        );
    }

    // Fecha corta de la reseña, por ejemplo "12/11/2026"
    formatDate(iso: string): string {
        const date = new Date(iso);
        return isNaN(date.getTime()) ? '' : date.toLocaleDateString('es-CO');
    }

    // Oculta una reseña visible o vuelve a mostrar una oculta
    toggleVisibility(review: Review) {
        this.reviewService.toggleReview(review.id).subscribe({
            next: (updated) => {
                this.reviewService.refresh();
                this.toast.show(
                    updated.visible
                        ? 'La reseña vuelve a mostrarse'
                        : 'La reseña quedó oculta',
                );
            },
            error: (error) => {
                console.error('Error cambiando la visibilidad:', error);
                this.errorTitle = 'No se pudo actualizar la reseña';
                this.errorMessage = getErrorMessage(
                    error,
                    'Ocurrió un error al actualizar la reseña. Por favor, inténtalo de nuevo.',
                );
                this.showErrorModal = true;
            },
        });
    }

    // Paso 1 de eliminar: guarda cuál es la reseña y abre la confirmación
    removeReview(review: Review) {
        this.selectedReviewId = review.id;
        this.showDeleteModal = true;
    }

    // El admin se arrepintió: se cierra la confirmación
    cancelDelete() {
        this.showDeleteModal = false;
        this.selectedReviewId = null;
    }

    // Paso 2 de eliminar: el admin confirmó
    confirmDelete() {
        if (this.selectedReviewId === null) {
            return;
        }

        const id = this.selectedReviewId;

        this.showDeleteModal = false;
        this.selectedReviewId = null;

        this.reviewService.deleteReview(id).subscribe({
            next: () => {
                this.reviewService.refresh();
                this.toast.show('Reseña eliminada correctamente');
            },
            error: (error) => {
                console.error('Error eliminando reseña:', error);
                this.errorTitle = 'No se pudo eliminar la reseña';
                this.errorMessage = getErrorMessage(
                    error,
                    'Ocurrió un error al eliminar la reseña. Por favor, inténtalo de nuevo.',
                );
                this.showErrorModal = true;
            },
        });
    }

    // Cierra la ventana de error y limpia sus textos
    closeErrorModal() {
        this.showErrorModal = false;
        this.errorTitle = '';
        this.errorMessage = '';
    }
}