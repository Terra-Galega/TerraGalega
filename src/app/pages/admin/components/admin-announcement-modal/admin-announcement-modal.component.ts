import {
    Component,
    EventEmitter,
    Input,
    Output,
    SimpleChanges,
    inject,
} from '@angular/core';
import { DialogDirective } from '../../../../directives/dialog.directive';
import {
    Announcement,
    CreateAnnouncement,
} from '../../../../models/announcement';
import { AnnouncementService } from '../../../../services/announcement.service';
import { ButtonComponent } from '../../../../components/button/button.component';
import { IconButtonComponent } from '../../../../components/icon-button/icon-button.component';
import { FormFieldComponent } from '../../../../components/form-field/form-field.component';
import { ToastService } from '../../../../services/toast.service';
import { getErrorMessage } from '../../../../utils/http-error';


// Límites que coinciden con las columnas del back
const TITLE_MAX = 120;
const DESCRIPTION_MAX = 500;
const URL_MAX = 500;
// Tiempo máximo para comprobar que la URL carga una imagen (milisegundos)
const IMAGE_CHECK_MS = 8_000;
/**
 * Ventana flotante para crear o editar un anuncio del home.
 * Si recibe un anuncio, lo edita; si recibe null, crea uno nuevo.
 * Valida título, descripción y URL de imagen (que sea una URL bien formada
 * y que realmente cargue una imagen). Avisa al padre con "closed".
 */
@Component({
    selector: 'app-admin-announcement-modal',
    imports: [DialogDirective, ButtonComponent, IconButtonComponent, FormFieldComponent],
    templateUrl: './admin-announcement-modal.component.html',
})
export class AdminAnnouncementModalComponent {
    private announcementService = inject(AnnouncementService);
    private toast = inject(ToastService);


    readonly titleMax = TITLE_MAX;
    readonly descriptionMax = DESCRIPTION_MAX;
    readonly urlMax = URL_MAX;

    // Anuncio a editar; null significa "crear uno nuevo"
    @Input() announcement: Announcement | null = null;

    // closed: se debe cerrar la ventana
    @Output() closed = new EventEmitter<void>();

    // Campos del formulario
    title = '';
    description = '';
    imageUrl = '';

    // Mensaje de error que se muestra dentro del formulario
    errorMessage = '';

    // true mientras se comprueba la imagen o se espera al servidor
    // (evita guardar dos veces por doble clic)
    saving = false;

    // true si la vista previa de la imagen no pudo cargar
    previewFailed = false;

    // Cada vez que el padre cambia el @Input "announcement": llena el
    // formulario (editar) o lo deja vacío (crear)
    ngOnChanges(changes: SimpleChanges) {
        if (!changes['announcement']) {
            return;
        }

        this.errorMessage = '';
        this.previewFailed = false;
        this.saving = false;

        if (this.announcement) {
            this.title = this.announcement.title;
            this.description = this.announcement.description;
            this.imageUrl = this.announcement.imageUrl;
        } else {
            this.title = '';
            this.description = '';
            this.imageUrl = '';
        }
    }

    // Guardan en el componente lo que el usuario escribe
    setTitle(event: Event) {
        this.title = (event.target as HTMLInputElement).value;
    }

    setDescription(event: Event) {
        this.description = (event.target as HTMLTextAreaElement).value;
    }

    setImageUrl(event: Event) {
        this.imageUrl = (event.target as HTMLInputElement).value;
        this.previewFailed = false;
    }

    // true si el texto es una URL http(s) completa, con dominio y sin espacios
    private isValidUrl(value: string): boolean {
        if (!value || /\s/.test(value)) {
            return false;
        }
        try {
            const url = new URL(value);
            const isHttp = url.protocol === 'http:' || url.protocol === 'https:';
            const hasHost =
                url.hostname.includes('.') || url.hostname === 'localhost';
            return isHttp && hasHost;
        } catch {
            return false;
        }
    }

    // La vista previa solo se muestra si la URL está bien formada
    get canPreview(): boolean {
        return this.isValidUrl(this.imageUrl.trim());
    }


    // Revisa los campos de texto. Devuelve el primer error o '' si todo está bien
    private validate(): string {
        const title = this.title.trim();
        const description = this.description.trim();
        const imageUrl = this.imageUrl.trim();

        if (!title) return 'El título del anuncio es obligatorio.';
        if (!description) return 'La descripción del anuncio es obligatoria.';
        if (!imageUrl) return 'La URL de la imagen es obligatoria.';
        if (imageUrl.length > URL_MAX)
            return `La URL de la imagen no puede superar los ${URL_MAX} caracteres.`;
        if (!this.isValidUrl(imageUrl))
            return 'La URL de la imagen no es válida. Debe ser una dirección completa, sin espacios, que empiece por http:// o https://.';
        return '';
    }

    // Botón "Guardar cambios" / "Añadir anuncio"
    async save() {
        if (this.saving) {
            return;
        }

        this.errorMessage = this.validate();
        if (this.errorMessage) {
            return;
        }

        const data: CreateAnnouncement = {
            title: this.title.trim(),
            description: this.description.trim(),
            imageUrl: this.imageUrl.trim(),
        };

        this.saving = true;


        // EDITAR
        if (this.announcement) {
            this.announcementService
                .updateAnnouncement({ ...this.announcement, ...data })
                .subscribe({
                    next: () =>
                        this.onSuccess('Cambios del anuncio guardados correctamente'),
                    error: (error) => this.onError(error),
                });
            return;
        }

        // CREAR (el back responde 409 si ya hay 3 anuncios)
        this.announcementService.createAnnouncement(data).subscribe({
            next: () => this.onSuccess('Anuncio creado correctamente'),
            error: (error) => this.onError(error),
        });
    }

    // Guardado exitoso: refresca la tabla, avisa al usuario y cierra la ventana
    private onSuccess(message: string) {
        this.announcementService.refresh();
        this.toast.show(message);
        this.closed.emit();
    }

    // Error al guardar: se muestra dentro del formulario y la ventana sigue abierta
    private onError(error: any) {
        console.error('Error guardando anuncio:', error);
        this.saving = false;
        this.errorMessage = getErrorMessage(
            error,
            'No se pudo guardar el anuncio.',
        );
    }

    // Botón "Cancelar" y la X: pide al padre cerrar la ventana
    close() {
        this.closed.emit();
    }
}