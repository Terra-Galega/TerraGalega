package com.example.demo.entities;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;
import com.fasterxml.jackson.annotation.JsonProperty;

// Genera automáticamente los métodos getter y setter para los atributos.
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
// Indica que esta clase representa una entidad persistente de JPA.
@Entity
// Define el nombre de la tabla asociada en la base de datos.
@Table(name = "admins")
public class Admin {

    // Identifica el atributo como clave primaria.
    @Id
    // Configura la generación automática del identificador mediante la base de datos.
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    // Establece restricciones para la columna del identificador.
    @Column(unique = true, nullable = false, length = 10)
    private Integer id;

    // Define el nombre del administrador como obligatorio, con un máximo de 70 caracteres.
    @Column(nullable = false, length = 70)
    private String name;

    // Define el apellido del administrador como obligatorio, con un máximo de 70 caracteres.
    @Column(nullable = false, length = 70)
    private String lastName;

    // Establece el correo como obligatorio y único para evitar duplicados en la tabla.
    @Column(unique = true, nullable = false, length = 120)
    private String email;

    // Permite recibir la contraseña en solicitudes, pero evita incluirla en las respuestas JSON.
    @JsonProperty(access = JsonProperty.Access.WRITE_ONLY)
    // Define la contraseña como un campo obligatorio de hasta 70 caracteres.
    @Column(nullable = false, length = 70)
    private String password;

    // Guarda el rol como texto en lugar de utilizar el valor numérico del enumerado.
    @Enumerated(EnumType.STRING)
    // Establece el rol como obligatorio, con un máximo de 20 caracteres.
    @Column(nullable = false, length = 20)
    private Role role;
}