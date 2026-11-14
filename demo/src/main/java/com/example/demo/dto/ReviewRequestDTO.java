package com.example.demo.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

// Lo que envía el cliente al crear o editar su reseña. El autor y la fecha
// los decide el back a partir del token.
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ReviewRequestDTO {

    // Estrellas enteras de 1 a 5
    private Integer rating;

    // Texto de la reseña (máximo 500 caracteres)
    private String comment;
}