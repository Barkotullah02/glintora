package com.glintora.backend.model;

import jakarta.persistence.*;
import lombok.*;
import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * Fast current-stock summary per (variant, warehouse). Maintained
 * transactionally whenever an {@link InventoryMovement} is posted.
 * quantity_on_hand does not account for reservations — available-to-sell
 * is quantity_on_hand minus active reserved quantity.
 */
@Entity
@Table(name = "inventory_balances")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@IdClass(InventoryBalanceKey.class)
public class InventoryBalance {

    @Id
    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "variant_id", nullable = false)
    private ProductVariant variant;

    @Id
    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "warehouse_id", nullable = false)
    private Warehouse warehouse;

    @Column(name = "quantity_on_hand", nullable = false)
    @Builder.Default
    private Integer quantityOnHand = 0;

    @Column(name = "updated_at")
    @Builder.Default
    private LocalDateTime updatedAt = LocalDateTime.now();
}
