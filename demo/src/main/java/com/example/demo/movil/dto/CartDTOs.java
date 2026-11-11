package com.example.demo.movil.dto;

import java.util.List;

import com.example.demo.dto.ProductSummaryDTO;

public final class CartDTOs {

    private CartDTOs() {
    }

    // Adicional dentro del carrito (igual que AddOnOption de la web)
    public record CartAddOnDTO(Integer id, String name, Double price) {
    }

    // Una línea del carrito. "id" es la misma llave que usa la web:
    // "productId:addOnId-addOnId"
    public record CartItemDTO(String id, ProductSummaryDTO product, int quantity, List<CartAddOnDTO> addOns) {
    }

    public record QuantityRequestDTO(int quantity) {
    }

    // Resultado de comparar los pedidos de H2 con su copia en Postgres
    public record MirrorCheckDTO(int h2Orders, int postgresOrders, List<Integer> mismatchedOrderIds) {
    }
}