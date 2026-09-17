package com.example.demo.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
@NoArgsConstructor
public class CheckoutItemDTO {

    private Integer productId;
    private Integer quantity;
    private List<Integer> addOnIds;
}