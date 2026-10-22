package com.example.demo.dto;

import lombok.Getter;
import lombok.Setter;
import lombok.NoArgsConstructor;

@Getter
@Setter
@NoArgsConstructor
public class ClientOrderDTO {
    private Integer id;
    private String name;
    private String lastName;
    private String email;
    private String phone;
}