package com.example.demo.entities;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;
import lombok.experimental.SuperBuilder;

@Getter
@Setter
@ToString
@AllArgsConstructor
@Entity
@NoArgsConstructor
@SuperBuilder
public class DeliveryPerson extends User {

    @Column(unique = true, nullable = false, length = 30)
    private String identification;
    @Column(nullable = false, unique = true, length = 25)
    private String phone;
    @Column(nullable = false)
    private Boolean available;
    @Column(nullable = false)
    private Boolean active;
}
