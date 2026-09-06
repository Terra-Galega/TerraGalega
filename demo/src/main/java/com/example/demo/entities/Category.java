package com.example.demo.entities;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

import java.util.List;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;

import java.util.ArrayList;

@Getter
@Setter
@ToString(exclude = { "product", "additionals" })
@AllArgsConstructor
@Entity
@NoArgsConstructor
@Builder
public class Category {
    @Id
    @Column(unique = true, nullable = false, length = 10)
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;
    @Column(unique = true, nullable = false, length = 70)
    private String name;
    @Column(nullable = true, length = 255)
    private String description;
    @Column(nullable = false)
    @OneToMany(mappedBy = "category")
    @Builder.Default
    private List<AddOn> additionals = new ArrayList<>();
    @OneToMany(mappedBy = "category")
    private List<Product> product = new ArrayList<>();
}
