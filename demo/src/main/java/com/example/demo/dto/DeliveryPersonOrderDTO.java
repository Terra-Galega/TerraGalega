package com.example.demo.dto;

import lombok.Getter;
import lombok.Setter;
import lombok.NoArgsConstructor;

@Getter
@Setter
@NoArgsConstructor
public class DeliveryPersonOrderDTO {
    private Integer id;
    private String name;
    private String lastName;
    private String phone;
}