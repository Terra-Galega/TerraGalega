package com.example.demo.controller;

import com.example.demo.entities.Announcement;
import com.example.demo.service.AnnouncementService;
import io.swagger.v3.oas.annotations.Operation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Controlador REST de los anuncios del home.
 * El GET lo usa el home (público); el resto lo usa el panel del admin.
 */
@RestController
@RequestMapping("/announcements")
// Permite las peticiones del front de Angular que corre en localhost:4200
@CrossOrigin(origins = "http://localhost:4200")
public class AnnouncementController {

    // La lógica de negocio (validaciones y límite de 3) vive en el servicio
    @Autowired
    private AnnouncementService announcementService;

    // GET /announcements -> todos los anuncios, en orden de creación
    @Operation(summary = "Get all announcements (max 3)")
    @GetMapping
    public List<Announcement> getAllAnnouncements() {
        return announcementService.getAllAnnouncements();
    }

    // POST /announcements -> crea un anuncio (409 si ya hay 3)
    @Operation(summary = "Add a new announcement (only for admin)")
    @PostMapping
    public Announcement addAnnouncement(@RequestBody Announcement announcement) {
        return announcementService.addAnnouncement(announcement);
    }

    // PUT /announcements/{id} -> actualiza título, descripción e imagen
    @Operation(summary = "Update an announcement (only for admin)")
    @PutMapping("/{id}")
    public Announcement updateAnnouncement(@PathVariable Integer id, @RequestBody Announcement announcement) {
        return announcementService.updateAnnouncement(id, announcement);
    }

    // DELETE /announcements/{id} -> elimina un anuncio
    @Operation(summary = "Delete an announcement (only for admin)")
    @DeleteMapping("/{id}")
    public void deleteAnnouncement(@PathVariable Integer id) {
        announcementService.deleteAnnouncement(id);
    }
}