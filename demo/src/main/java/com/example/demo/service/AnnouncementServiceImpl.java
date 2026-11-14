package com.example.demo.service;

import com.example.demo.entities.Announcement;
import com.example.demo.errors.ApiException;
import com.example.demo.repository.AnnouncementRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.List;

/**
 * Lógica de negocio de los anuncios: validaciones de los campos y el límite
 * de MAX_ANNOUNCEMENTS anuncios publicados a la vez.
 */
@Service
public class AnnouncementServiceImpl implements AnnouncementService {

    // Acceso a la tabla de anuncios
    @Autowired
    private AnnouncementRepository repository;

    // Todos los anuncios en orden de creación
    @Override
    @Transactional
    public List<Announcement> getAllAnnouncements() {
        return repository.findAllByOrderByIdAsc();
    }

    // Validaciones compartidas por addAnnouncement() y updateAnnouncement().
    // También deja los textos sin espacios sobrantes.
    private void validateAnnouncement(Announcement announcement) {
        if (announcement.getTitle() == null || announcement.getTitle().isBlank()) {
            throw ApiException.badRequest("El título del anuncio no puede estar vacío.");
        }
        if (announcement.getTitle().trim().length() > 120) {
            throw ApiException.badRequest("El título no puede superar los 120 caracteres.");
        }
        if (announcement.getDescription() == null || announcement.getDescription().isBlank()) {
            throw ApiException.badRequest("La descripción del anuncio no puede estar vacía.");
        }
        if (announcement.getDescription().trim().length() > 500) {
            throw ApiException.badRequest("La descripción no puede superar los 500 caracteres.");
        }
        if (announcement.getImageUrl() == null || announcement.getImageUrl().isBlank()) {
            throw ApiException.badRequest("La URL de la imagen no puede estar vacía.");
        }
        String url = announcement.getImageUrl().trim();
        if (url.length() > 500) {
            throw ApiException.badRequest("La URL de la imagen no puede superar los 500 caracteres.");
        }
        if (!isValidHttpUrl(url)) {
            throw ApiException.badRequest(
                    "La URL de la imagen no es válida. Debe ser una dirección completa que empiece por http:// o https://.");
        }

        announcement.setTitle(announcement.getTitle().trim());
        announcement.setDescription(announcement.getDescription().trim());
        announcement.setImageUrl(url);
    }

    // true si el texto es una URL http(s) con dominio (o localhost) y sin espacios
    private boolean isValidHttpUrl(String value) {
        try {
            URI uri = new URI(value);
            String scheme = uri.getScheme();
            String host = uri.getHost();
            return ("http".equalsIgnoreCase(scheme) || "https".equalsIgnoreCase(scheme))
                    && host != null
                    && (host.contains(".") || host.equalsIgnoreCase("localhost"));
        } catch (URISyntaxException e) {
            // Espacios u otros caracteres inválidos
            return false;
        }
    }

    // Crea un anuncio nuevo si todavía hay cupo
    @Override
    @Transactional
    public Announcement addAnnouncement(Announcement announcement) {
        validateAnnouncement(announcement);

        if (repository.count() >= MAX_ANNOUNCEMENTS) {
            throw ApiException.conflict(
                    "Solo puedes tener hasta " + MAX_ANNOUNCEMENTS
                            + " anuncios. Elimina uno para poder crear otro.");
        }

        // Se fuerza id nulo para que siempre se cree uno nuevo
        announcement.setId(null);
        return repository.save(announcement);
    }

    // Actualiza los tres campos de un anuncio existente
    @Override
    @Transactional
    public Announcement updateAnnouncement(Integer id, Announcement announcement) {
        Announcement existing = repository.findById(id)
                .orElseThrow(() -> ApiException.notFound("El anuncio no existe."));

        validateAnnouncement(announcement);

        existing.setTitle(announcement.getTitle());
        existing.setDescription(announcement.getDescription());
        existing.setImageUrl(announcement.getImageUrl());

        return repository.save(existing);
    }

    // Elimina un anuncio
    @Override
    @Transactional
    public void deleteAnnouncement(Integer id) {
        if (!repository.existsById(id)) {
            throw ApiException.notFound("El anuncio no existe.");
        }
        repository.deleteById(id);
    }
}