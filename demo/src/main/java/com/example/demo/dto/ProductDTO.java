package com.example.demo.dto;

import lombok.Getter;
import lombok.Setter;
import lombok.NoArgsConstructor;

@Getter
@Setter
@NoArgsConstructor
public class ProductDTO {
    private Integer id;
    private String name;
    private String description;
    private Double price;
    private CategoryDTO category;
    private String imageUrl;
    private Boolean active;
    private Boolean popular;
    private boolean vegetarian;
    private boolean spicyMild;
    private boolean spicyHot;
    private boolean containsNuts;
    private boolean containsSeafood;
    private boolean containsGluten;
}