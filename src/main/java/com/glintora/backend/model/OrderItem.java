package com.glintora.backend.model;

import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;

/**
 * One line of an order. unit_price and unit_cost are snapshots taken at
 * purchase time so historical revenue and profit stay accurate even if
 * product prices change later.
 */
@Entity
@Table(name = "order_items")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OrderItem {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "order_id", nullable = false)
    private Order order;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "variant_id", nullable = false)
    private ProductVariant variant;

    @Column(nullable = false)
    private Integer quantity;

    /** Selling price snapshot at order time. */
    @Column(name = "unit_price", nullable = false)
    private BigDecimal unitPrice;

    /** Cost snapshot at order time. */
    @Column(name = "unit_cost", nullable = false)
    private BigDecimal unitCost;

    @Column(name = "discount_amount", nullable = false)
    @Builder.Default
    private BigDecimal discountAmount = BigDecimal.ZERO;
}
