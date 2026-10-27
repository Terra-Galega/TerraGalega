package com.example.demo.entities;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.persistence.Id;
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
@ToString(exclude = "password")
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
    // Configura la generación automática del identificador mediante la base de datos.
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    // Define el identificador como obligatorio y único.
    @Column(unique = true, nullable = false, length = 10)
    private Integer id;

    // Almacena el nombre del cliente; es obligatorio y admite hasta 70 caracteres.
    @Column(nullable = false, length = 70)
    private String name;

    // Almacena el apellido del cliente; es obligatorio y admite hasta 70 caracteres.
    @Column(nullable = false, length = 70)
    private String lastName;

    // Almacena el correo electrónico; es obligatorio y único en la tabla.
    @Column(unique = true, nullable = false, length = 120)
    private String email;

    // Permite recibir la contraseña en las solicitudes, pero evita incluirla en las respuestas JSON.
    @JsonProperty(access = JsonProperty.Access.WRITE_ONLY)
    // Define la contraseña como obligatoria, con un máximo de 70 caracteres.
    @Column(nullable = false, length = 70)
    private String password;

    // Indica el rol del usuario y lo almacena como texto en la base de datos.
    @Enumerated(EnumType.STRING)
    // Establece el rol como obligatorio, con un máximo de 20 caracteres.
    @Column(nullable = false, length = 20)
    private Role role;

    // Almacena el número de teléfono; debe ser único y no puede estar vacío.
    @Column(unique = true, nullable = false, length = 25)
    private String phone;

    // Almacena la dirección del cliente como un campo obligatorio de hasta 150 caracteres.
    @Column(nullable = false, length = 150)
    private String address;
}