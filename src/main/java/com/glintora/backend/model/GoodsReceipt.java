package com.glintora.backend.model;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Records stock physically received into a warehouse. Posting a receipt
 * creates receipt inventory movements and increases inventory balances.
 */
@Entity
@Table(name = "goods_receipts")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class GoodsReceipt {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "shipment_id")
    private Shipment shipment;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "warehouse_id", nullable = false)
    private Warehouse warehouse;

    @Column(name = "received_at", updatable = false)
    @Builder.Default
    private LocalDateTime receivedAt = LocalDateTime.now();

    @Column(name = "received_by")
    private String receivedBy;

    @OneToMany(mappedBy = "goodsReceipt", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<GoodsReceiptItem> items = new ArrayList<>();
}
