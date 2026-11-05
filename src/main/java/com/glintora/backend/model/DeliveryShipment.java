package com.glintora.backend.model;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

/** Courier delivery tracking for a customer order. */
@Entity
@Table(name = "delivery_shipments")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DeliveryShipment {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "order_id", nullable = false)
    private Order order;

    private String courier;

    @Column(name = "tracking_number", length = 100)
    private String trackingNumber;

    /** preparing, picked_up, in_transit, delivered, returned */
    @Column(nullable = false)
    @Builder.Default
    private String status = "preparing";

    @Column(name = "shipped_at")
    private LocalDateTime shippedAt;

    @Column(name = "delivered_at")
    private LocalDateTime deliveredAt;
}
