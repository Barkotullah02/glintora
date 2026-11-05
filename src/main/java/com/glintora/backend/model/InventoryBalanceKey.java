package com.glintora.backend.model;

import lombok.*;
import java.io.Serializable;
import java.util.Objects;

/** Composite primary key of inventory_balances: (variant_id, warehouse_id). */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class InventoryBalanceKey implements Serializable {
    private Long variant;
    private Long warehouse;

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof InventoryBalanceKey that)) return false;
        return Objects.equals(variant, that.variant) && Objects.equals(warehouse, that.warehouse);
    }

    @Override
    public int hashCode() {
        return Objects.hash(variant, warehouse);
    }
}
