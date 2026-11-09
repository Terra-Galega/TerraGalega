package com.example.demo.entities;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

// Dirección de entrega guardada por un cliente.
@Getter
@Setter
// "client" se excluye para evitar ciclos y cargas innecesarias.
@ToString(exclude = "client")
@AllArgsConstructor
@NoArgsConstructor
@Builder
@Entity
@Table(name = "addresses")
public class Address {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(unique = true, nullable = false)
    private Integer id;

    // Nombre con el que el cliente identifica la dirección (ej. "Casa", "Oficina").
    @Column(nullable = false, length = 40)
    private String name;

    // Texto de la dirección. 120 para ser consistente con Order.address.
    @Column(nullable = false, length = 120)
    private String label;

    // true en la dirección predeterminada. Cada cliente tiene máximo una.
    // (la columna no se llama "default" porque es palabra reservada de SQL)
    @Builder.Default
    @Column(name = "default_address", nullable = false)
    private Boolean defaultAddress = false;

    // FK al cliente dueño de la dirección. Se ignora en el JSON para evitar
    // la recursión Client -> Address -> Client.
    @JsonIgnore
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "client_id", nullable = false)
    private Client client;
}