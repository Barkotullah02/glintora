package com.glintora.backend.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.math.BigDecimal;

/** Card view of a product for storefront listings. */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ProductCard {
    private Long id;
    private String productCode;
    private String name;
    private String brandName;
    private String categoryName;
    private BigDecimal price;
    private String imageUrl;
    /** Units available to sell across all warehouses (on hand - reserved). */
    private int availableUnits;
    private boolean inStock;
}
