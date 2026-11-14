package com.example.demo.repository;

import com.example.demo.entities.Review;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Acceso a datos de las reseñas. Extiende JpaRepository, así que ya trae
 * guardar, buscar por id, listar y borrar.
 */
@Repository
public interface ReviewRepository extends JpaRepository<Review, Integer> {

    // La reseña de un cliente (como mucho una)
    Optional<Review> findByClientId(Integer clientId);

    // Reseñas visibles, de la más reciente a la más antigua (home y Nosotros)
    List<Review> findByVisibleTrueOrderByCreatedAtDesc();

    // Todas las reseñas, visibles u ocultas (vista del admin)
    List<Review> findAllByOrderByCreatedAtDesc();

    // Borra la reseña de un cliente (al eliminar su cuenta)
    void deleteByClientId(Integer clientId);
}