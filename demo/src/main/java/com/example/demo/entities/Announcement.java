package com.example.demo.entities;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Entidad Announcement: un anuncio que el admin publica en el home.
 * Tiene título, descripción y la URL de una imagen. Máximo 3 a la vez
 * (la regla vive en AnnouncementServiceImpl).
 */
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
@Entity
public class Announcement {

    // Clave primaria, generada automáticamente por la base de datos
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(unique = true, nullable = false, length = 10)
    private Integer id;

    // Título del anuncio (obligatorio)
    @Column(nullable = false, length = 120)
    private String title;

    // Texto del anuncio (obligatorio)
    @Column(nullable = false, length = 500)
    private String description;

    // Dirección de la imagen (obligatoria)
    @Column(nullable = false, length = 500)
    private String imageUrl;
}