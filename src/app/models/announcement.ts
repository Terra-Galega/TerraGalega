// Cantidad máxima de anuncios publicados a la vez (el back también lo valida)
export const MAX_ANNOUNCEMENTS = 3;

// Anuncio del home, tal como lo manda el back
export interface Announcement {
  id: number;
  title: string;
  description: string;
  imageUrl: string;
}

// Datos para crear un anuncio: sin id, porque lo genera el back
export type CreateAnnouncement = Omit<Announcement, 'id'>;