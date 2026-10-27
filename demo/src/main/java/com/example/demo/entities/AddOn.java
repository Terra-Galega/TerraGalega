package com.example.demo.entities;

import java.util.HashSet;
import java.util.Set;

import com.fasterxml.jackson.annotation.JsonIncludeProperties;
import com.fasterxml.jackson.annotation.JsonCreator;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToMany;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

/**
 * Entidad AddOn: un adicional que el cliente puede agregar a un plato
 * (por ejemplo, queso extra). Se guarda en la tabla "add_on".
 *
 * Tiene una relación muchos a muchos con Category: un adicional puede aparecer
 * en varias categorías y una categoría puede tener varios adicionales.
 * AddOn es el lado dueño de esa relación.
 */
// Lombok: genera getters, setters y toString (sin las categorías, para no
// cargar la relación al imprimir el objeto)
@Getter
@Setter
@ToString(exclude = { "categories" })
// Constructor con todos los campos. El @JsonCreator DISABLED evita que Jackson
// lo use al leer JSON: así usa el constructor vacío y los setters.
@AllArgsConstructor(onConstructor_ = @JsonCreator(mode = JsonCreator.Mode.DISABLED))
@Entity
// Constructor vacío, obligatorio para JPA
@NoArgsConstructor
// Permite crear objetos con AddOn.builder()...build()
@Builder
public class AddOn {

    // Clave primaria, generada automáticamente por la base de datos
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(unique = true, nullable = false, length = 10)
    private Integer id;

    // Nombre del adicional (obligatorio)
    @Column(nullable = false, length = 120)
    private String name;

    // Descripción opcional
    @Column(nullable = true, length = 255)
    private String description;

    // Precio del adicional (puede ser 0 si es gratuito)
    @Column(nullable = false)
    private Double price;

    // true = disponible para los clientes; false = oculto pero conservado
    // (Boolean con mayúscula para poder detectar cuando no se envía)
    @Column(nullable = false)
    private Boolean active;

    // Lado dueño del ManyToMany (crea la tabla intermedia addon_category)
    @ManyToMany
    @JoinTable(name = "addon_category", joinColumns = @JoinColumn(name = "addon_id"), inverseJoinColumns = @JoinColumn(name = "category_id"))
    // Al convertir a JSON, de cada categoría solo se incluyen id y nombre
    @JsonIncludeProperties({ "id", "name" })
    // Hace que el builder también inicialice el conjunto vacío por defecto
    @Builder.Default
    private Set<Category> categories = new HashSet<>();
}