package com.example.demo.entities;

import java.util.ArrayList;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnore;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

// Mesa del restaurante. No tiene CRUD: las mesas están "quemadas" según el plano
// SVG de la página de reservas y se cargan al arrancar (DiningTableLoader).
// Relación con Reservation: una mesa tiene de 0 a muchas reservas (cada una en
// una fecha y hora distinta) y cada reserva tiene exactamente una mesa. La FK
// vive en Reservation.table (columna dining_table_id).
@Getter
@Setter
@ToString(exclude = "reservations")
@AllArgsConstructor
@NoArgsConstructor
@Builder
@Entity
@Table(name = "dining_tables")
public class DiningTable {

    // Número visible de la mesa en el plano (101, 102... piso 1; 201... piso 2).
    // No se genera: es el mismo número que dibuja el SVG.
    @Id
    @Column(nullable = false)
    private Integer id;

    // Piso del restaurante (1 o 2)
    @Column(nullable = false)
    private Integer floor;

    // Si la mesa está en servicio. La ocupación en una fecha y hora concreta no
    // se guarda aquí: se calcula con sus reservas.
    @Column(nullable = false)
    private Boolean available;

    // Número de sillas (en las mesas de celebración, el máximo que admite)
    @Column(nullable = false)
    private Integer seats;

    // Pedidos de mesa: true cuando ya se le entregó todo lo pedido (no le queda
    // ningún pedido en curso). Vuelve a false al registrar un pedido nuevo.
    @Column(nullable = false)
    @Builder.Default
    private Boolean attended = false;

    // Reservas de la mesa a lo largo del tiempo. No se envía en el JSON para
    // evitar la recursión DiningTable -> Reservation -> DiningTable.
    @JsonIgnore
    @Builder.Default
    @OneToMany(mappedBy = "table")
    private List<Reservation> reservations = new ArrayList<>();
}
