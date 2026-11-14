package com.example.demo.controller;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import com.example.demo.dto.ReviewDTO;
import com.example.demo.dto.ReviewRequestDTO;
import com.example.demo.security.AuthenticatedUser;
import com.example.demo.service.ReviewService;

import io.swagger.v3.oas.annotations.Operation;

/**
 * Controlador REST de las reseñas.
 * - GET /reviews: público (home y Nosotros).
 * - /reviews/me: el cliente con sesión; el cliente sale del token.
 * - /reviews/all, /reviews/{id}/toggle y DELETE /reviews/{id}: solo admin.
 */
@RestController
@RequestMapping("/reviews")
@CrossOrigin(origins = "http://localhost:4200")
public class ReviewController {

    @Autowired
    private ReviewService reviewService;

    @Autowired
    private AuthenticatedUser authenticatedUser;

    // GET /reviews -> reseñas visibles, de la más reciente a la más antigua
    @Operation(summary = "Get the visible reviews (public)")
    @GetMapping
    public List<ReviewDTO> getVisibleReviews() {
        return reviewService.getVisibleReviews();
    }

    // GET /reviews/me -> mi reseña (204 si todavía no tengo)
    @Operation(summary = "Get the review of the logged-in client")
    @GetMapping("/me")
    public ResponseEntity<ReviewDTO> getMyReview() {
        ReviewDTO review = reviewService.getMyReview(authenticatedUser.client().getId());
        return review == null ? ResponseEntity.noContent().build() : ResponseEntity.ok(review);
    }

    // PUT /reviews/me -> crea mi reseña o actualiza la que ya tengo
    @Operation(summary = "Create or update the review of the logged-in client")
    @PutMapping("/me")
    public ReviewDTO saveMyReview(@RequestBody ReviewRequestDTO request) {
        return reviewService.saveMyReview(authenticatedUser.client().getId(), request);
    }

    // DELETE /reviews/me -> elimina mi reseña
    @Operation(summary = "Delete the review of the logged-in client")
    @DeleteMapping("/me")
    public void deleteMyReview() {
        reviewService.deleteMyReview(authenticatedUser.client().getId());
    }

    // GET /reviews/all -> todas, visibles y ocultas
    @Operation(summary = "Get all reviews, including hidden ones (only for admin)")
    @GetMapping("/all")
    public List<ReviewDTO> getAllReviews() {
        return reviewService.getAllReviews();
    }

    // PUT /reviews/{id}/toggle -> oculta o vuelve a mostrar una reseña
    @Operation(summary = "Hide or show a review (only for admin)")
    @PutMapping("/{id}/toggle")
    public ReviewDTO toggleVisibility(@PathVariable Integer id) {
        return reviewService.toggleVisibility(id);
    }

    // DELETE /reviews/{id} -> elimina una reseña
    @Operation(summary = "Delete a review (only for admin)")
    @DeleteMapping("/{id}")
    public void deleteReview(@PathVariable Integer id) {
        reviewService.deleteReview(id);
    }
}