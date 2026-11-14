package com.example.demo.entities;

import java.time.LocalDateTime;

import com.fasterxml.jackson.annotation.JsonIgnore;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

/**
 * Reseña de un cliente sobre el restaurante: calificación de 1 a 5 estrellas
 * y un comentario. Cada cliente puede tener como máximo una (la columna
 * client_id es única). El admin puede ocultarla con "visible".
 */
@Getter
@Setter
// "client" se excluye para evitar cargas innecesarias en el toString
@ToString(exclude = "client")
@AllArgsConstructor
@NoArgsConstructor
@Builder
@Entity
@Table(name = "reviews")
public class Review {

    // Clave primaria, generada automáticamente por la base de datos
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(unique = true, nullable = false)
    private Integer id;

    // Autor de la reseña. unique = true garantiza una sola reseña por cliente
    // aunque alguien se salte las validaciones del servicio.
    @JsonIgnore
    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "client_id", nullable = false, unique = true)
    private Client client;

    // Estrellas enteras, de 1 a 5 (se valida en ReviewServiceImpl)
    @Column(nullable = false)
    private Integer rating;

    // Texto de la reseña. La columna no se llama "comment" para evitar
    // palabras reservadas de SQL.
    @Column(name = "comment_text", nullable = false, length = 500)
    private String comment;

    // Fecha en que se creó la reseña
    @Column(nullable = false)
    private LocalDateTime createdAt;

    // false = el admin la ocultó y ya no sale en el home ni en Nosotros
    @Builder.Default
    @Column(nullable = false)
    private Boolean visible = true;
}