package com.glintora.backend.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.math.BigDecimal;

/** One resolved line in the shopping cart. */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CartLine {
    private Long variantId;
    private String sku;
    private Long productId;
    private String productCode;
    private String productName;
    private String sizeName;
    private String colorName;
    private String imageUrl;
    private BigDecimal unitPrice;
    private int quantity;
    private int available;   // remaining available-to-sell excluding this line
    private boolean inStock;

    public BigDecimal lineTotal() {
        return unitPrice == null ? BigDecimal.ZERO : unitPrice.multiply(BigDecimal.valueOf(quantity));
    }
}
