package com.example.demo.entities;

import com.fasterxml.jackson.annotation.JsonProperty;

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

/**
 * Entidad que representa a un repartidor (domiciliario) del restaurante.
 * Se guarda en la tabla "delivery_persons".
 */
@Getter
@Setter
@ToString
@AllArgsConstructor
@NoArgsConstructor
@Builder
@Entity
@Table(name = "delivery_persons")
public class DeliveryPerson {

    // Identificador del repartidor, autogenerado por la base de datos.  
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(unique = true, nullable = false, length = 10)
    private Integer id;

    @Column(nullable = false, length = 70)
    private String name;

    @Column(nullable = false, length = 70)
    private String lastName;

    // Correo único; se usa también para iniciar sesión.  
    @Column(unique = true, nullable = false, length = 120)
    private String email;

    // WRITE_ONLY: la contraseña se puede recibir en un JSON (al crear la cuenta)
    // pero nunca se devuelve en las respuestas de la API
    @JsonProperty(access = JsonProperty.Access.WRITE_ONLY)
    @Column(nullable = false, length = 70)
    private String password;

    // Rol del usuario; para esta entidad siempre es DELIVERY_PERSON.  
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private Role role;

    // Documento de identidad, único entre repartidores.  
    @Column(unique = true, nullable = false, length = 30)
    private String identification;

    // Teléfono de contacto, único entre repartidores.  
    @Column(nullable = false, unique = true, length = 25)
    private String phone;

    // Disponibilidad operativa: pasa a false mientras lleva un pedido y vuelve a true cuando lo entrega
    @Column(nullable = false)
    private Boolean available;

    // Estado administrativo: el admin lo activa/desactiva (por ejemplo si ese día no trabaja). 
    // Un repartidor inactivo no puede recibir pedidos.
    @Column(nullable = false)
    private Boolean active;
}