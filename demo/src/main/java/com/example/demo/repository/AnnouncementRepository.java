package com.example.demo.repository;

import com.example.demo.entities.Announcement;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * Acceso a datos de los anuncios. Extiende JpaRepository, así que ya trae
 * guardar, buscar por id, listar, contar y borrar.
 */
@Repository
public interface AnnouncementRepository extends JpaRepository<Announcement, Integer> {

    // Todos los anuncios en el orden en que se crearon (el carrusel los
    // muestra en ese orden)
    List<Announcement> findAllByOrderByIdAsc();
}