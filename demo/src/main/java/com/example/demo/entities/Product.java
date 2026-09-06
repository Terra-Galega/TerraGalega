package com.example.demo.entities;

import org.hibernate.annotations.ManyToAny;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
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
@Entity
@NoArgsConstructor
@Builder
public class Product {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(unique = true, nullable = false, length = 10)
    private Integer id;
    @Column(nullable = false, length = 120)
    private String name;
    @Column(nullable = true, length = 255)
    private String description;
    @Column(nullable = false, length = 255)
    private Double price;
    @JoinColumn(nullable = false)
    @ManyToOne
    private Category category;
    @Column(nullable = true, length = 255)
    private String imageUrl;
    @Column(nullable = false)
    private Boolean active;
    @Column(nullable = false)
    private Boolean popular;
}