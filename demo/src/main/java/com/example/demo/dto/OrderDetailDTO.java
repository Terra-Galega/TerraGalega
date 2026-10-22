package com.example.demo.dto;

import java.util.List;
import lombok.Getter;
import lombok.Setter;
import lombok.NoArgsConstructor;

@Getter
@Setter
@NoArgsConstructor
public class OrderDetailDTO {
    private Integer id;
    private ProductDTO product;
    private Integer quantity;
    private Double unitPrice;
    private List<AddOnOrderDTO> addOns;
}