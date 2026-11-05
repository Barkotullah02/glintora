package com.glintora.backend.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

/**
 * One row of the admin stock overview: a variant sitting in a specific
 * warehouse, with the numbers and status the guide's stock table requires.
 */
@Data
@AllArgsConstructor
public class StockRow {
    private Long variantId;
    private String sku;
    private String productName;
    private String sizeName;
    private String colorName;
    private Long warehouseId;
    private String warehouseName;
    private int onHand;
    private int reserved;
    /** on hand − reserved at this location (never below zero). */
    private int available;
    /** in, low or out — low when at/below the variant's reorder point. */
    private String status;
    private boolean reorderRequired;
}
