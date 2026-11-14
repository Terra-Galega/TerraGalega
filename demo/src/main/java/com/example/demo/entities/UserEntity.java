package com.example.demo.entities;

import java.util.ArrayList;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnore;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

// Tabla única con las credenciales de TODOS los tipos de usuario
// (clientes, admins, operadores y domiciliarios). Cada uno de ellos tiene un
// OneToOne hacia esta tabla; los datos propios de cada tipo siguen en su tabla.
@Getter
@Setter
@ToString(exclude = { "password", "roles" })
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Table(name = "users")
public class UserEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    // Identificador con el que se entra al sistema: el correo
    @Column(unique = true, nullable = false, length = 120)
    private String username;

    // Contraseña cifrada con BCrypt (siempre 60 caracteres). Nunca sale en un JSON.
    @JsonIgnore
    @Column(nullable = false, length = 100)
    private String password;

    // Un usuario puede tener varios roles y un rol pertenece a muchos usuarios.
    // EAGER: al traer el usuario siempre se traen sus roles (Spring Security los necesita).
    // Sin cascade: borrar un usuario solo borra sus filas en user_roles, no el rol.
    @JsonIgnore
    @ManyToMany(fetch = FetchType.EAGER)
    @JoinTable(name = "user_roles",
            joinColumns = @JoinColumn(name = "user_id"),
            inverseJoinColumns = @JoinColumn(name = "role_id"))
    @Builder.Default
    private List<RoleEntity> roles = new ArrayList<>();
}