package com.glintora.backend.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.math.BigDecimal;

/** A variant with its live availability, used on the product page. */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class VariantView {
    private Long id;
    private String sku;
    private Long sizeId;
    private String sizeName;
    private Long colorId;
    private String colorName;
    private String colorHex;
    private BigDecimal price;
    private int onHand;
    private int reserved;
    private int available;
    private boolean availableNow;
}
