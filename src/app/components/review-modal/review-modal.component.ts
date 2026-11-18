import { TranslatePipe } from '../../services/translate.pipe';
import {
    Component,
    EventEmitter,
    HostListener,
    Input,
    OnInit,
    Output,
    inject,
} from '@angular/core';
import { DialogDirective } from '../../directives/dialog.directive';
import { ButtonComponent } from '../button/button.component';
import { IconButtonComponent } from '../icon-button/icon-button.component';
import { FormFieldComponent } from '../form-field/form-field.component';
import { ModalComponent } from '../modal/modal.component';
import { StarIconComponent } from '../star-icon/star-icon.component';
import { Review } from '../../models/review';
import { ReviewService } from '../../services/review.service';
import { ToastService } from '../../services/toast.service';
import { getErrorMessage } from '../../utils/http-error';

// Límite que coincide con la columna del back
const COMMENT_MAX = 500;

// Texto que acompaña a cada calificación (la posición 0 es la calificación 1)
const RATING_LABELS = ['Muy mala', 'Mala', 'Regular', 'Buena', 'Excelente'];

/**
 * Ventana flotante para escribir o editar la reseña del cliente con sesión.
 * Si recibe una reseña, la edita (y permite eliminarla); si recibe null, crea
 * una nueva. Califica con 1 a 5 estrellas enteras y un comentario. Avisa al
 * padre con "closed".
 */
@Component({
    selector: 'app-review-modal',
    imports: [DialogDirective, 
        ButtonComponent,
        IconButtonComponent,
        FormFieldComponent,
        ModalComponent,
        StarIconComponent,
    TranslatePipe],
    templateUrl: './review-modal.component.html',
})
export class ReviewModalComponent implements OnInit {
    private reviewService = inject(ReviewService);
    private toast = inject(ToastService);

    readonly commentMax = COMMENT_MAX;
    readonly stars = [1, 2, 3, 4, 5];

    // Reseña a editar; null significa "escribir una nueva"
    @Input() review: Review | null = null;

    // closed: se debe cerrar la ventana
    @Output() closed = new EventEmitter<void>();

    // Campos del formulario
    rating = 0;
    comment = '';
    // Estrella sobre la que está el mouse (0 = ninguna); solo es una vista previa
    hovered = 0;

    // Mensaje de error que se muestra dentro del formulario
    errorMessage = '';

    // true mientras se espera al servidor (evita guardar dos veces por doble clic)
    saving = false;

    // true mientras se pide confirmar la eliminación
    confirmDelete = false;

    // Llena el formulario si se está editando una reseña
    ngOnInit() {
        if (this.review) {
            this.rating = this.review.rating;
            this.comment = this.review.comment;
        }
    }

    // Estrellas que se pintan llenas: la vista previa del mouse o la calificación elegida
    get shown(): number {
        return this.hovered || this.rating;
    }

    // Texto de la calificación mostrada ("Excelente", "Buena"...)
    get ratingLabel(): string {
        return this.shown ? RATING_LABELS[this.shown - 1] : '';
    }

    setRating(value: number) {
        this.rating = value;
        this.hovered = 0;
        this.errorMessage = '';
    }

    setComment(event: Event) {
        this.comment = (event.target as HTMLTextAreaElement).value;
    }

    // Revisa los campos. Devuelve el primer error o '' si todo está bien
    private validate(): string {
        if (this.rating < 1 || this.rating > 5) {
            return 'Elige una calificación de 1 a 5 estrellas.';
        }
        const comment = this.comment.trim();
        if (!comment) return 'Escribe un comentario.';
        if (comment.length > COMMENT_MAX) {
            return `El comentario no puede superar los ${COMMENT_MAX} caracteres.`;
        }
        return '';
    }

    // Botón "Publicar reseña" / "Guardar cambios"
    save() {
        if (this.saving) {
            return;
        }

        this.errorMessage = this.validate();
        if (this.errorMessage) {
            return;
        }

        this.saving = true;

        this.reviewService
            .saveMyReview({ rating: this.rating, comment: this.comment.trim() })
            .subscribe({
                next: () => {
                    const editing = this.review !== null;
                    this.reviewService.refresh();
                    this.toast.show(
                        editing ? 'Reseña actualizada' : '¡Gracias por tu reseña!',
                        editing
                            ? 'Tus cambios ya se ven en la página.'
                            : 'Ya aparece en la página.',
                    );
                    this.closed.emit();
                },
                error: (error) => {
                    console.error('Error guardando reseña:', error);
                    this.saving = false;
                    this.errorMessage = getErrorMessage(
                        error,
                        'No se pudo guardar tu reseña.',
                    );
                },
            });
    }

    // Confirmó eliminar su reseña
    remove() {
        if (this.saving) {
            return;
        }

        this.saving = true;
        this.confirmDelete = false;

        this.reviewService.deleteMyReview().subscribe({
            next: () => {
                this.reviewService.refresh();
                this.toast.show('Reseña eliminada');
                this.closed.emit();
            },
            error: (error) => {
                console.error('Error eliminando reseña:', error);
                this.saving = false;
                this.errorMessage = getErrorMessage(
                    error,
                    'No se pudo eliminar tu reseña.',
                );
            },
        });
    }

    // Botón "Cancelar" y la X: pide al padre cerrar la ventana
    close() {
        if (!this.saving) {
            this.closed.emit();
        }
    }

    // Escape cierra la ventana, salvo que esté abierta la confirmación de eliminar
    // (esa se cierra con su propio Escape)
    @HostListener('document:keydown.escape')
    onEscape() {
        if (!this.confirmDelete) {
            this.close();
        }
    }
}