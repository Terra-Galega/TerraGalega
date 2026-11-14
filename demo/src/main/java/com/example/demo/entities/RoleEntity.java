package com.example.demo.entities;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

// Tabla de roles: CLIENT, ADMIN, OPERATOR, DELIVERY_PERSON.
// Los nombres salen del enum Role para que coincidan siempre.
@Getter
@Setter
@ToString
@NoArgsConstructor
@Entity
@Table(name = "roles")
public class RoleEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(unique = true, nullable = false, length = 30)
    private String name;

    // Constructor con un solo parámetro para crear los roles en el DataLoader
    public RoleEntity(String name) {
        this.name = name;
    }
}