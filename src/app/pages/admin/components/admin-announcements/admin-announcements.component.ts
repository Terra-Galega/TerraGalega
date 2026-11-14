import {
    Component,
    EventEmitter,
    Input,
    OnChanges,
    Output,
    inject,
} from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { EmptyStateComponent } from '../../../../components/empty-state/empty-state.component';
import { IconButtonComponent } from '../../../../components/icon-button/icon-button.component';
import { ModalComponent } from '../../../../components/modal/modal.component';
import {
    Announcement,
    MAX_ANNOUNCEMENTS,
} from '../../../../models/announcement';
import { AnnouncementService } from '../../../../services/announcement.service';
import { ToastService } from '../../../../services/toast.service';
import { getErrorMessage } from '../../../../utils/http-error';

/**
 * Tabla de anuncios del home en el panel de administración.
 * Muestra cada anuncio con su imagen, permite filtrar por título, editar y
 * eliminar. Indica cuántos de los MAX_ANNOUNCEMENTS permitidos están usados.
 */
@Component({
    selector: 'app-admin-announcements',
    imports: [EmptyStateComponent, IconButtonComponent, ModalComponent],
    templateUrl: './admin-announcements.component.html',
})
export class AdminAnnouncementsComponent implements OnChanges {
    private announcementService = inject(AnnouncementService);
    private toast = inject(ToastService);

    // Texto de búsqueda que escribe el admin en la barra superior
    @Input() searchTerm = '';

    // Avisa al padre qué anuncio se quiere editar (el padre abre el modal)
    @Output() editAnnouncement = new EventEmitter<Announcement>();

    // Encabezados de la tabla
    readonly columns = ['Imagen', 'Título', 'Descripción', 'Acciones'];

    // Máximo de anuncios permitidos (para el contador)
    readonly max = MAX_ANNOUNCEMENTS;

    // Todos los anuncios y los que coinciden con la búsqueda
    announcements: Announcement[] = [];
    filteredAnnouncements: Announcement[] = [];

    // Controlan la ventana de confirmación y la ventana de error
    showDeleteModal = false;
    showErrorModal = false;

    // Id del anuncio que se está a punto de eliminar
    selectedAnnouncementId: number | null = null;

    // Textos de la ventana de confirmación
    confirmTitle = 'Eliminar anuncio';
    confirmMessage = '¿Estás seguro de que deseas eliminar este anuncio del inicio?';

    // Textos de la ventana de error
    errorTitle = '';
    errorMessage = '';

    constructor() {
        this.loadAnnouncements();

        // Cada vez que otro componente pide refrescar (por ejemplo, tras guardar
        // en el modal), se vuelve a cargar la lista
        this.announcementService.refresh$
            .pipe(takeUntilDestroyed())
            .subscribe(() => this.loadAnnouncements());
    }

    // Pide los anuncios al servidor y vuelve a aplicar el filtro
    loadAnnouncements() {
        this.announcementService.getAnnouncements().subscribe({
            next: (announcements) => {
                this.announcements = announcements;
                this.filterAnnouncements();
            },
            error: (error) => {
                console.error('Error cargando anuncios:', error);
            },
        });
    }

    // Angular lo llama cuando cambia un @Input (aquí, searchTerm)
    ngOnChanges() {
        this.filterAnnouncements();
    }

    // Deja solo los anuncios cuyo título contiene el texto buscado
    filterAnnouncements() {
        const value = this.searchTerm.trim().toLowerCase();

        this.filteredAnnouncements = this.announcements.filter((announcement) =>
            announcement.title.toLowerCase().includes(value),
        );
    }

    // Reenvía al padre el anuncio que se quiere editar
    openEdit(announcement: Announcement) {
        this.editAnnouncement.emit(announcement);
    }

    // Paso 1 de eliminar: guarda cuál es el anuncio y abre la confirmación
    removeAnnouncement(announcement: Announcement) {
        this.selectedAnnouncementId = announcement.id;
        this.showDeleteModal = true;
    }

    // El admin se arrepintió: se cierra la confirmación
    cancelDelete() {
        this.showDeleteModal = false;
        this.selectedAnnouncementId = null;
    }

    // Paso 2 de eliminar: el admin confirmó
    confirmDelete() {
        if (this.selectedAnnouncementId === null) {
            return;
        }

        const id = this.selectedAnnouncementId;

        this.showDeleteModal = false;
        this.selectedAnnouncementId = null;

        this.announcementService.deleteAnnouncement(id).subscribe({
            next: () => {
                this.announcementService.refresh();
                this.toast.show('Anuncio eliminado correctamente');
            },
            error: (error) => {
                console.error('Error eliminando anuncio:', error);
                this.errorTitle = 'No se pudo eliminar el anuncio';
                this.errorMessage = getErrorMessage(
                    error,
                    'Ocurrió un error al eliminar el anuncio. Por favor, inténtalo de nuevo.',
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