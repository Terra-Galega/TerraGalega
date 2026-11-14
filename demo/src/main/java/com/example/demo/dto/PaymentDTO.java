package com.example.demo.dto;

public record PaymentDTO(
        Integer orderId,
        String state,
        String checkoutUrl,
        Long amountInCents,
        String currency) {
}