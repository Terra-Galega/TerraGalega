package com.example.demo.service;

import com.example.demo.entities.Announcement;

import java.util.List;

/**
 * Contrato de la lógica de negocio de los anuncios del home.
 * La implementación está en AnnouncementServiceImpl.
 */
public interface AnnouncementService {

    // Cantidad máxima de anuncios publicados al mismo tiempo
    int MAX_ANNOUNCEMENTS = 3;

    // Todos los anuncios, en orden de creación
    List<Announcement> getAllAnnouncements();

    // Crea un anuncio; falla si ya hay MAX_ANNOUNCEMENTS
    Announcement addAnnouncement(Announcement announcement);

    // Actualiza título, descripción e imagen de un anuncio existente
    Announcement updateAnnouncement(Integer id, Announcement announcement);

    // Elimina un anuncio
    void deleteAnnouncement(Integer id);
}