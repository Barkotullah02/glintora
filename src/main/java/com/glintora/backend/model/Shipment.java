package com.glintora.backend.model;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDate;

/**
 * An incoming shipment tied to a purchase order. Stock only increases when
 * goods are physically received against a shipment, never when a purchase
 * order is merely created.
 */
@Entity
@Table(name = "shipments")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Shipment {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "purchase_order_id", nullable = false)
    private PurchaseOrder purchaseOrder;

    @Column(name = "shipment_reference", length = 100)
    private String shipmentReference;

    @Column(name = "dispatch_date")
    private LocalDate dispatchDate;

    @Column(name = "expected_arrival_date")
    private LocalDate expectedArrivalDate;

    @Column(nullable = false)
    @Builder.Default
    private String status = "dispatched";
}
