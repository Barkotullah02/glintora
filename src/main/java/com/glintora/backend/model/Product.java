package com.glintora.backend.model;

import com.glintora.backend.model.enums.ProductStatus;
import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * A product as customers recognise it. The purchasable units live in
 * {@link ProductVariant}. Stock availability is never represented by the
 * publication status; it is always derived from inventory movements.
 */
@Entity
@Table(name = "products")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Product {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "product_code", nullable = false, unique = true)
    private String productCode;

    @Column(nullable = false)
    private String name;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "brand_id")
    private Brand brand;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "category_id")
    private Category category;

    @Column(columnDefinition = "TEXT")
    private String description;

    private String material;

    /**
     * Publication status only. A published product can still be out of stock;
     * availability is computed from inventory_balances and stock_reservations.
     */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Builder.Default
    private ProductStatus status = ProductStatus.draft;

    /** Default selling price; a variant may override it with its own price. */
    @Column(name = "default_selling_price")
    private BigDecimal defaultSellingPrice;

    /** Latest known unit cost; snapshotted onto order items at sale time. */
    @Column(name = "cost_price")
    private BigDecimal costPrice;

    @OneToMany(mappedBy = "product", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    @OrderBy("id ASC")
    private List<ProductVariant> variants = new ArrayList<>();

    @OneToMany(mappedBy = "product", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    @OrderBy("sortOrder ASC, id ASC")
    private List<ProductImage> images = new ArrayList<>();

    @OneToMany(mappedBy = "product", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<ProductAttribute> attributes = new ArrayList<>();

    @OneToMany(mappedBy = "product", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<ProductMeasurement> measurements = new ArrayList<>();

    @Column(name = "created_at", updatable = false)
    @Builder.Default
    private LocalDateTime createdAt = LocalDateTime.now();

    @Column(name = "updated_at")
    @Builder.Default
    private LocalDateTime updatedAt = LocalDateTime.now();

    /** Effective price used when a variant has no price override. */
    public BigDecimal effectivePrice() {
        return defaultSellingPrice;
    }
}
