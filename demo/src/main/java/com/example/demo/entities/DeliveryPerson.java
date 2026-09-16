package com.example.demo.entities;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.MapsId;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;
@Getter
@Setter
@ToString
@AllArgsConstructor
@NoArgsConstructor
@Builder
@Entity
@Table(name = "delivery_persons")
public class DeliveryPerson {

    @Id
    private Integer id;

    @OneToOne(cascade = CascadeType.ALL)
    @MapsId
    @JoinColumn(name = "id")
    private UserRole userRole;

    @Column(unique = true, nullable = false, length = 30)
    private String identification;

    @Column(nullable = false, unique = true, length = 25)
    private String phone;

    @Column(nullable = false)
    private Boolean available;

    @Column(nullable = false)
    private Boolean active;
}