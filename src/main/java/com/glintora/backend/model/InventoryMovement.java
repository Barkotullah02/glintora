package com.glintora.backend.model;

import com.glintora.backend.model.enums.MovementType;
import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

/**
 * Permanent audit history of every stock change. Quantity is signed:
 * positive for stock in, negative for stock out. Never update or delete
 * rows here — post compensating movements instead.
 */
@Entity
@Table(name = "inventory_movements")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class InventoryMovement {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "variant_id", nullable = false)
    private ProductVariant variant;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "warehouse_id", nullable = false)
    private Warehouse warehouse;

    @Enumerated(EnumType.STRING)
    @Column(name = "movement_type", nullable = false, length = 50)
    private MovementType movementType;

    /** Signed: positive = stock in, negative = stock out. */
    @Column(nullable = false)
    private Integer quantity;

    /** e.g. goods_receipt, order, adjustment */
    @Column(name = "reference_type", length = 50)
    private String referenceType;

    @Column(name = "reference_id")
    private Long referenceId;

    /** Who posted the movement (staff name or system actor). */
    @Column(length = 100)
    private String actor;

    /** Why it was posted — damage, count correction, receipt notes… */
    @Column(columnDefinition = "text")
    private String reason;

    @Column(name = "created_at", updatable = false)
    @Builder.Default
    private LocalDateTime createdAt = LocalDateTime.now();
}
