package com.example.demo.service;

import com.example.demo.dto.ReviewDTO;
import com.example.demo.dto.ReviewRequestDTO;
import com.example.demo.entities.Client;
import com.example.demo.entities.Review;
import com.example.demo.errors.ApiException;
import com.example.demo.repository.ClientRepository;
import com.example.demo.repository.ReviewRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Lógica de negocio de las reseñas: validación de estrellas y comentario,
 * una sola reseña por cliente y moderación del admin.
 */
@Service
public class ReviewServiceImpl implements ReviewService {

    // Acceso a la tabla de reseñas
    @Autowired
    private ReviewRepository repository;

    // Para obtener el cliente autor de la reseña
    @Autowired
    private ClientRepository clientRepository;

    // Reseñas visibles para el home y Nosotros
    @Override
    @Transactional
    public List<ReviewDTO> getVisibleReviews() {
        return repository.findByVisibleTrueOrderByCreatedAtDesc().stream().map(this::toDTO).toList();
    }

    // Todas las reseñas para el panel del admin
    @Override
    @Transactional
    public List<ReviewDTO> getAllReviews() {
        return repository.findAllByOrderByCreatedAtDesc().stream().map(this::toDTO).toList();
    }

    // Reseña del cliente; null si no tiene
    @Override
    @Transactional
    public ReviewDTO getMyReview(Integer clientId) {
        return repository.findByClientId(clientId).map(this::toDTO).orElse(null);
    }

    // Crea la reseña o actualiza la existente (una por cliente).
    // Al editar se conserva la fecha original y el estado visible/oculto.
    @Override
    @Transactional
    public ReviewDTO saveMyReview(Integer clientId, ReviewRequestDTO request) {
        validate(request);

        Client client = clientRepository.findById(clientId)
                .orElseThrow(() -> ApiException.notFound("El cliente no existe."));

        Review review = repository.findByClientId(clientId)
                .orElseGet(() -> Review.builder()
                        .client(client)
                        .createdAt(LocalDateTime.now())
                        .visible(true)
                        .build());

        review.setRating(request.getRating());
        review.setComment(request.getComment().trim());

        return toDTO(repository.save(review));
    }

    // El cliente elimina su propia reseña
    @Override
    @Transactional
    public void deleteMyReview(Integer clientId) {
        Review review = repository.findByClientId(clientId)
                .orElseThrow(() -> ApiException.notFound("Aún no has dejado una reseña."));
        repository.delete(review);
    }

    // Admin: oculta una reseña visible o muestra una oculta
    @Override
    @Transactional
    public ReviewDTO toggleVisibility(Integer id) {
        Review review = repository.findById(id)
                .orElseThrow(() -> ApiException.notFound("La reseña no existe."));
        review.setVisible(!review.getVisible());
        return toDTO(repository.save(review));
    }

    // Admin: elimina una reseña
    @Override
    @Transactional
    public void deleteReview(Integer id) {
        if (!repository.existsById(id)) {
            throw ApiException.notFound("La reseña no existe.");
        }
        repository.deleteById(id);
    }

    // Se usa al borrar la cuenta del cliente, para no dejar la FK colgando
    @Override
    @Transactional
    public void deleteClientReview(Integer clientId) {
        repository.deleteByClientId(clientId);
    }

    // Validaciones de las estrellas y el comentario
    private void validate(ReviewRequestDTO request) {
        if (request == null || request.getRating() == null
                || request.getRating() < 1 || request.getRating() > 5) {
            throw ApiException.badRequest("La calificación debe ser un número entero de 1 a 5.");
        }
        if (request.getComment() == null || request.getComment().isBlank()) {
            throw ApiException.badRequest("El comentario no puede estar vacío.");
        }
        if (request.getComment().trim().length() > MAX_COMMENT_LENGTH) {
            throw ApiException.badRequest(
                    "El comentario no puede superar los " + MAX_COMMENT_LENGTH + " caracteres.");
        }
    }

    // Convierte la entidad en el DTO que ve el front
    private ReviewDTO toDTO(Review review) {
        return new ReviewDTO(
                review.getId(),
                review.getRating(),
                review.getComment(),
                displayName(review.getClient()),
                review.getCreatedAt(),
                review.getVisible());
    }

    // "María López" -> "María L."; solo se muestra la inicial del apellido
    private String displayName(Client client) {
        String lastName = client.getLastName();
        if (lastName == null || lastName.isBlank()) {
            return client.getName();
        }
        return client.getName() + " " + lastName.trim().charAt(0) + ".";
    }
}