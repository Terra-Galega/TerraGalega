package com.example.demo.service;

import com.example.demo.dto.ReviewDTO;
import com.example.demo.dto.ReviewRequestDTO;

import java.util.List;

/**
 * Contrato de la lógica de negocio de las reseñas.
 * La implementación está en ReviewServiceImpl.
 */
public interface ReviewService {

    // Largo máximo del comentario
    int MAX_COMMENT_LENGTH = 500;

    // Reseñas visibles, de la más reciente a la más antigua (público)
    List<ReviewDTO> getVisibleReviews();

    // Todas las reseñas, visibles u ocultas (admin)
    List<ReviewDTO> getAllReviews();

    // Reseña del cliente, o null si todavía no ha escrito una
    ReviewDTO getMyReview(Integer clientId);

    // Crea la reseña del cliente o, si ya tenía una, la actualiza
    ReviewDTO saveMyReview(Integer clientId, ReviewRequestDTO request);

    // Elimina la reseña del cliente
    void deleteMyReview(Integer clientId);

    // Admin: alterna una reseña entre visible y oculta
    ReviewDTO toggleVisibility(Integer id);

    // Admin: elimina cualquier reseña
    void deleteReview(Integer id);

    // Borra la reseña de un cliente, si la tiene (al eliminar su cuenta)
    void deleteClientReview(Integer clientId);
}