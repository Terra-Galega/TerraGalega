package com.example.demo.dto;

import java.time.LocalDateTime;
import java.util.UUID;
import com.example.demo.entities.PointsMovement.Kind;

public class PointsDTOs {
    public record Movement(Integer orderId, Kind kind, long amount, LocalDateTime createdAt) {}
    public record Quote(boolean subscribed, long balance, long required, double total, boolean canPay) {}
    public record Payment(UUID requestId, CheckoutRequestDTO checkout, long expectedPoints, double expectedTotal) {}
}
