package com.example.demo.dto;

import java.util.Set;
import lombok.Getter;
import lombok.Setter;
import lombok.NoArgsConstructor;

@Getter
@Setter
@NoArgsConstructor
public class AddOnDTO {
    private Integer id;
    private String name;
    private String description;
    private Double price;
    private Boolean active;
    private Set<CategoryRefDTO> categories;
}