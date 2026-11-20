import { Celebration } from './celebration';

// Vista del carrusel: el anuncio festivo es local y no ocupa un cupo del backend.
export interface AnnouncementSlide {
  key: string;
  title: string;
  description: string;
  imageUrl: string | null;
  celebration: Celebration | null;
}