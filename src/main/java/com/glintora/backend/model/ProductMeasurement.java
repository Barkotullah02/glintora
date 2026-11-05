package com.glintora.backend.model;

import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;

/**
 * One measurement row per product/size/section, used to build size charts
 * on the storefront. Replaces the workbook's one-column-per-size layout.
 */
@Entity
@Table(name = "product_measurements")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ProductMeasurement {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_id", nullable = false)
    private Product product;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "size_id", nullable = false)
    private Size size;

    /** e.g. Shirt, Trouser, Three Piece */
    @Column(name = "garment_section", nullable = false, length = 100)
    private String garmentSection;

    /** e.g. Length, Chest, Body, Waist, Sleeve */
    @Column(name = "measurement_name", nullable = false, length = 100)
    private String measurementName;

    @Column(name = "measurement_value", nullable = false)
    private BigDecimal measurementValue;

    @Column(nullable = false)
    @Builder.Default
    private String unit = "inch";
}
