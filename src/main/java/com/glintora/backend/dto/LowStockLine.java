package com.glintora.backend.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/** A variant that has fallen to or below its reorder point. */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class LowStockLine {
    private Long ruleId;
    private Long variantId;
    private String sku;
    private String productName;
    private String sizeName;
    private String colorName;
    private int onHand;
    private int reserved;
    private int available;
    private int outstandingOrdered;
    private int reorderPoint;
    private int targetQuantity;
    /** max(0, target - (available + outstanding ordered)) */
    private int suggestedReorderQty;
}
