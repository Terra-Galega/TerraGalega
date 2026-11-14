package com.example.demo.entities;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.CascadeType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Transient;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.persistence.Id;
import jakarta.persistence.CascadeType;
import jakarta.persistence.OneToMany;
import java.util.ArrayList;
import java.util.List;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

// Genera automáticamente los métodos getter y setter.
@Getter
@Setter
// Genera el método toString(), excluyendo la contraseña para evitar mostrarla.
@ToString(exclude = { "password", "addresses", "user" })
// Genera un constructor con todos los atributos.
@AllArgsConstructor
// Genera un constructor sin argumentos.
@NoArgsConstructor
// Permite construir objetos mediante el patrón Builder.
@Builder
// Indica que esta clase representa una entidad gestionada por JPA.
@Entity
// Define el nombre de la tabla en la base de datos.
@Table(name = "clients")
public class Client {

    // Identifica el atributo como clave primaria.
    @Id
    // Configura la generación automática del identificador mediante la base de
    // datos.
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    // Define el identificador como obligatorio y único.
    @Column(unique = true, nullable = false, length = 10)
    private Integer id;

    // Almacena el nombre del cliente; es obligatorio y admite hasta 70 caracteres.
    @Column(nullable = false, length = 70)
    private String name;

    // Almacena el apellido del cliente; es obligatorio y admite hasta 70
    // caracteres.
    @Column(nullable = false, length = 70)
    private String lastName;

    // Almacena el correo electrónico; es obligatorio y único en la tabla.
    @Column(unique = true, nullable = false, length = 120)
    private String email;

    // Se recibe en el registro pero NO se guarda en esta tabla (@Transient):
    // la contraseña cifrada vive en users. Nunca sale en las respuestas.
    @JsonProperty(access = JsonProperty.Access.WRITE_ONLY)
    @Transient
    private String password;

    // Indica el rol del usuario y lo almacena como texto en la base de datos.
    @Enumerated(EnumType.STRING)
    // Establece el rol como obligatorio, con un máximo de 20 caracteres.
    @Column(nullable = false, length = 20)
    private Role role;

    // Almacena el número de teléfono; debe ser único y no puede estar vacío.
    @Column(unique = true, nullable = false, length = 25)
    private String phone;

    // Direcciones guardadas del cliente. La FK vive en Address.client.
    // cascade + orphanRemoval: al borrar el cliente o quitar una dirección
    // de la lista, se borra también en la base de datos.
    @OneToMany(mappedBy = "client", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<Address> addresses = new ArrayList<>();

        // Credenciales del cliente en la tabla users (correo + contraseña + roles).
    // cascade ALL: al borrar el cliente se borra también su usuario.
    @JsonIgnore
    @OneToOne(cascade = CascadeType.ALL)
    @JoinColumn(name = "user_id", unique = true)
    private UserEntity user;
}