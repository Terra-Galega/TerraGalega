package com.example.demo.entities;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import tools.jackson.databind.ser.jdk.NumberSerializers.IntegerSerializer;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class Category {
    private Integer id;
    private String name;
    private String description;
    private List<AddOn> additionals;
}
