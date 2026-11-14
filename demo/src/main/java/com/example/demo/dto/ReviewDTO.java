package com.example.demo.dto;

import java.time.LocalDateTime;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

// Reseña tal como la recibe el front. El autor llega ya armado ("María L.")
// para no exponer el apellido completo ni el correo.
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ReviewDTO {

    private Integer id;

    // Estrellas de 1 a 5
    private Integer rating;

    private String comment;

    // Nombre y inicial del apellido, por ejemplo "María L."
    private String authorName;

    private LocalDateTime createdAt;

    // false = oculta (solo la ve el admin)
    private Boolean visible;
}