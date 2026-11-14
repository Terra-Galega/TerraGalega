package com.example.demo.dto;

import com.example.demo.entities.Operator;
import com.example.demo.entities.Role;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

// Datos de un operador que se envían al front (panel del admin y del operador).
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class OperatorDTO {

    private Integer id;
    private String name;
    private String lastName;
    private String email;
    private String username;
    private Role role;
    private Boolean active;

    public static OperatorDTO from(Operator operator) {
        return new OperatorDTO(operator.getId(), operator.getName(), operator.getLastName(),
                operator.getEmail(), operator.getUsername(), operator.getRole(), operator.getActive());
    }
}
