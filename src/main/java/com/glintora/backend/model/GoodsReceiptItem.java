package com.glintora.backend.model;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "goods_receipt_items")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class GoodsReceiptItem {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "goods_receipt_id", nullable = false)
    private GoodsReceipt goodsReceipt;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "variant_id", nullable = false)
    private ProductVariant variant;

    @Column(name = "quantity_received", nullable = false)
    private Integer quantityReceived;

    @Column(name = "quantity_rejected", nullable = false)
    @Builder.Default
    private Integer quantityRejected = 0;

    /** quantity_received - quantity_rejected; the amount added to stock. */
    public Integer quantityAccepted() {
        return quantityReceived - (quantityRejected == null ? 0 : quantityRejected);
    }
}
