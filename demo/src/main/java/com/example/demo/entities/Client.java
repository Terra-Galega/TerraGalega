package com.example.demo.entities;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class Client {
    private Integer id;
    private String name;
    private String lastName;
    private String email;
    private String password;
    private String phone;
    private String address;
    private Boolean active;
    private Boolean admin;

}