package com.example.demo.entities;

import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

//Constructores, getters y setters generados automáticamente por Lombok
// (Constructor vacío tambien)
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Product {
    private Integer id;
    private String name;
    private String description;
    private Double price;
    private String category;
    private String imageUrl;
    private List<AddOn> additionals;
    private Boolean Active;
    private Boolean popular; //Indica si el producto aparece en "Favoritos de la casa" (home)

}