package com.example.demo.entities;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

//Constructores, getters y setters generados automáticamente por Lombok
// (Constructor vacío tambien)
@Data
@NoArgsConstructor
@AllArgsConstructor
public class AddOn {
    private Integer id;
    private String name;
    private String description;
    private Double price;
    private Boolean Active;
}