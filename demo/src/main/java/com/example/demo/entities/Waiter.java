package com.example.demo.entities;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import jakarta.persistence.Transient;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

// Mesero del salón. Registra los pedidos de mesa desde la app móvil e inicia
// sesión con su correo (rol WAITER), igual que el operador y el domiciliario.
// Relación con TableOrder: un mesero tiene muchos pedidos (la FK vive en
// TableOrder.waiter); así la cocina sabe a quién avisar y queda trazabilidad.
@Getter
@Setter
@ToString(exclude = { "password", "user" })
@AllArgsConstructor
@NoArgsConstructor
@Builder
@Entity
@Table(name = "waiters")
public class Waiter {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(unique = true, nullable = false, length = 10)
    private Integer id;

    @Column(nullable = false, length = 70)
    private String name;

    @Column(nullable = false, length = 70)
    private String lastName;

    // Correo único; se usa también para iniciar sesión
    @Column(unique = true, nullable = false, length = 120)
    private String email;

    // Se recibe al crear la cuenta pero no se guarda aquí: vive cifrada en users
    @JsonProperty(access = JsonProperty.Access.WRITE_ONLY)
    @Transient
    private String password;

    // Rol del usuario; para esta entidad siempre es WAITER
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private Role role;

    // Estado administrativo: un mesero inactivo no puede iniciar sesión
    @Column(nullable = false)
    private Boolean active;

    // Credenciales del mesero en la tabla users
    @JsonIgnore
    @OneToOne(cascade = CascadeType.ALL)
    @JoinColumn(name = "user_id", unique = true)
    private UserEntity user;
}
