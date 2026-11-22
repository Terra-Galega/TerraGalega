package com.example.demo.entities;

import java.time.LocalDateTime;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Getter
@NoArgsConstructor
@Table(name = "points_movements", uniqueConstraints = @UniqueConstraint(columnNames = {"order_id", "kind"}))
public class PointsMovement {
    public enum Kind { EARNED, SPENT, RETURNED }

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "order_id")
    private Order order;
    @Enumerated(EnumType.STRING) @Column(nullable = false)
    private Kind kind;
    private long amount;
    private LocalDateTime createdAt;

    public PointsMovement(Order order, Kind kind, long amount) {
        this.order = order;
        this.kind = kind;
        this.amount = amount;
        this.createdAt = LocalDateTime.now();
    }
}
