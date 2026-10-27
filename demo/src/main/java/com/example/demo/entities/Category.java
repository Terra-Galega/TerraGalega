package com.example.demo.entities;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonCreator;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.OneToMany;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

/**
 * Entidad Category: una categoría de la carta (por ejemplo, entradas o
 * postres).
 * Agrupa productos y define qué adicionales se pueden ofrecer con ellos.
 *
 * Se relaciona con AddOn (muchos a muchos, donde AddOn es el dueño) y con
 * Product (uno a muchos: una categoría tiene varios productos).
 */
// Lombok: genera getters, setters y toString (sin las relaciones, para evitar
// ciclos infinitos y cargas innecesarias al imprimir)
@Getter
@Setter
@ToString(exclude = { "product", "addOns" })
// Constructor con todos los campos; el @JsonCreator DISABLED evita que Jackson
// lo use al leer JSON (usa el constructor vacío y los setters)
@AllArgsConstructor(onConstructor_ = @JsonCreator(mode = JsonCreator.Mode.DISABLED))
@Entity
// Constructor vacío, obligatorio para JPA
@NoArgsConstructor
// Permite crear objetos con Category.builder()...build()
@Builder
public class Category {

    // Clave primaria, generada automáticamente por la base de datos
    @Id
    @Column(unique = true, nullable = false, length = 10)
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    // Nombre de la categoría: obligatorio y no se puede repetir
    @Column(unique = true, nullable = false, length = 70)
    private String name;

    // Descripción opcional
    @Column(nullable = true, length = 255)
    private String description;

    // Lado inverso del ManyToMany: el dueño de la relación es AddOn.
    // @JsonIgnore evita que los adicionales viajen dentro del JSON de la categoría.
    @ManyToMany(mappedBy = "categories")
    @JsonIgnore
    @Builder.Default
    private Set<AddOn> addOns = new HashSet<>();

    // Productos de esta categoría (el dueño de la relación es Product.category).
    // También se oculta del JSON para no devolver todos los productos.
    @OneToMany(mappedBy = "category")
    @JsonIgnore
    @Builder.Default
    private List<Product> product = new ArrayList<>();
}