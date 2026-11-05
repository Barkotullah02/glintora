package com.glintora.backend.model;

import com.glintora.backend.model.enums.ProductVariantStatus;
import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * A specific purchasable version of a product (size + color combination).
 * Every SKU has its own inventory; customers always order variants, never
 * bare products.
 */
@Entity
@Table(name = "product_variants")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ProductVariant {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_id", nullable = false)
    private Product product;

    @Column(nullable = false, unique = true)
    private String sku;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "size_id")
    private Size size;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "color_id")
    private Color color;

    /** Weight in grams. */
    private BigDecimal weight;

    /** Optional price override; when null the product's default price applies. */
    @Column(name = "selling_price")
    private BigDecimal sellingPrice;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Builder.Default
    private ProductVariantStatus status = ProductVariantStatus.active;

    @Column(name = "created_at", updatable = false)
    @Builder.Default
    private LocalDateTime createdAt = LocalDateTime.now();

    /** Price actually charged for this variant. */
    public BigDecimal effectiveSellingPrice() {
        if (sellingPrice != null) {
            return sellingPrice;
        }
        return product != null ? product.getDefaultSellingPrice() : null;
    }
}
