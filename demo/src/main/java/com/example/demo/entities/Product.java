package com.example.demo.entities;

import com.fasterxml.jackson.annotation.JsonIncludeProperties;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

/**
 * Entidad Product: un plato de la carta del restaurante.
 * Cada producto pertenece obligatoriamente a una categoría y tiene indicadores
 * (vegetariano, picante, alérgenos) que usa el menú para filtrar.
 */
// Lombok: genera getters, setters y toString (sin la categoría, para no
// cargar la relación al imprimir el objeto)
@Getter
@Setter
@ToString(exclude = { "category" })
// Constructor con todos los campos
@AllArgsConstructor
@Entity
// Constructor vacío, obligatorio para JPA
@NoArgsConstructor
// Permite crear objetos con Product.builder()...build()
@Builder
public class Product {

    // Clave primaria, generada automáticamente por la base de datos
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(unique = true, nullable = false, length = 10)
    private Integer id;

    // Nombre del plato (obligatorio)
    @Column(nullable = false, length = 120)
    private String name;

    // Descripción opcional
    @Column(nullable = true, length = 255)
    private String description;

    // Precio del plato
    @Column(nullable = false, length = 255)
    private Double price;

    // Categoría del plato (obligatoria: un producto no puede quedar sin ella).
    // Al convertir a JSON solo se incluyen id y nombre de la categoría.
    @JsonIncludeProperties({ "id", "name" })
    @JoinColumn(nullable = false)
    @ManyToOne
    private Category category;

    // Dirección de la imagen del plato (opcional)
    @Column(nullable = true, length = 300)
    private String imageUrl;

    // true = visible en el menú; false = oculto pero conservado.
    // (Boolean con mayúscula para poder detectar cuando no se envía)
    @Column(nullable = false)
    private Boolean active;

    // true = aparece en "Favoritos de la casa" en el inicio
    @Column(nullable = false)
    private Boolean popular;

    // Características del plato que se usan como filtros en el menú
    @Column(nullable = false)
    private boolean vegetarian;

    @Column(nullable = false)
    private boolean spicyMild;

    @Column(nullable = false)
    private boolean spicyHot;

    // Alérgenos: contiene nueces, mariscos o gluten
    @Column(nullable = false)
    private boolean containsNuts;

    @Column(nullable = false)
    private boolean containsSeafood;

    @Column(nullable = false)
    private boolean containsGluten;

}