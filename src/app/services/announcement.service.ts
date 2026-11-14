import { API_URL } from '../api-url';
import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable, Subject } from 'rxjs';
import { Announcement, CreateAnnouncement } from '../models/announcement';

/**
 * Servicio que habla con el back en /announcements (anuncios del home).
 * Es un servicio único (providedIn: 'root'), compartido por toda la app.
 */
@Injectable({
    providedIn: 'root',
})
export class AnnouncementService {
    // Dirección base de los endpoints de anuncios en el back
    private apiUrl = `${API_URL}/announcements`;

    // Aviso de "los anuncios cambiaron": quien modifica uno llama a refresh()
    // y los componentes que escuchan refresh$ recargan su lista
    private refreshSubject = new Subject<void>();

    refresh$ = this.refreshSubject.asObservable();

    constructor(private http: HttpClient) { }

    // Dispara el aviso para que las listas se vuelvan a cargar
    refresh() {
        this.refreshSubject.next();
    }

    // Todos los anuncios (máximo 3), en orden de creación
    getAnnouncements(): Observable<Announcement[]> {
        return this.http.get<Announcement[]>(this.apiUrl);
    }

    // Crea un anuncio nuevo (el back responde 409 si ya hay 3)
    createAnnouncement(announcement: CreateAnnouncement): Observable<Announcement> {
        return this.http.post<Announcement>(this.apiUrl, announcement);
    }

    // Actualiza un anuncio existente
    updateAnnouncement(announcement: Announcement): Observable<Announcement> {
        return this.http.put<Announcement>(
            `${this.apiUrl}/${announcement.id}`,
            announcement,
        );
    }

    // Elimina un anuncio
    deleteAnnouncement(id: number): Observable<void> {
        return this.http.delete<void>(`${this.apiUrl}/${id}`);
    }
}