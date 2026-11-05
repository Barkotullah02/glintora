package com.glintora.backend.model;

import jakarta.persistence.*;
import lombok.*;

/**
 * Variant-level replenishment rule. Reorder logic is per variant because
 * different sizes sell at different rates.
 */
@Entity
@Table(name = "inventory_reorder_rules",
        uniqueConstraints = @UniqueConstraint(columnNames = {"variant_id", "warehouse_id"}))
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class InventoryReorderRule {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "variant_id", nullable = false)
    private ProductVariant variant;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "warehouse_id", nullable = false)
    private Warehouse warehouse;

    @Column(name = "reorder_point", nullable = false)
    @Builder.Default
    private Integer reorderPoint = 2;

    @Column(name = "target_quantity", nullable = false)
    @Builder.Default
    private Integer targetQuantity = 30;
}
