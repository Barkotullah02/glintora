package com.glintora.backend.model;

import com.glintora.backend.model.enums.ReservationStatus;
import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

/**
 * Stock allocated to a customer order item. A reservation reduces the
 * quantity available for others to buy but does not reduce physical
 * on-hand stock — that happens at dispatch, via a sale_dispatch movement.
 */
@Entity
@Table(name = "stock_reservations")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class StockReservation {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "order_item_id")
    private OrderItem orderItem;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "variant_id", nullable = false)
    private ProductVariant variant;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "warehouse_id", nullable = false)
    private Warehouse warehouse;

    @Column(nullable = false)
    private Integer quantity;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Builder.Default
    private ReservationStatus status = ReservationStatus.active;

    @Column(name = "expires_at")
    private LocalDateTime expiresAt;
}
