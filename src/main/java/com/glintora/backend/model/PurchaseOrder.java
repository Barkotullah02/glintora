package com.glintora.backend.model;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "purchase_orders")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PurchaseOrder {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "supplier_id", nullable = false)
    private Supplier supplier;

    @Column(name = "order_date", nullable = false)
    private LocalDate orderDate;

    /** draft, ordered, partial, received, cancelled */
    @Column(nullable = false)
    @Builder.Default
    private String status = "draft";

    @Column(nullable = false)
    @Builder.Default
    private String currency = "BDT";

    @Column(name = "expected_date")
    private LocalDate expectedDate;

    @OneToMany(mappedBy = "purchaseOrder", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<PurchaseOrderItem> items = new ArrayList<>();

    @Column(name = "created_at", updatable = false)
    @Builder.Default
    private LocalDateTime createdAt = LocalDateTime.now();

    /** Sum of ordered quantity × unit cost across all lines. */
    public java.math.BigDecimal totalValue() {
        java.math.BigDecimal total = java.math.BigDecimal.ZERO;
        if (items == null) return total;
        for (PurchaseOrderItem item : items) {
            if (item.getQuantityOrdered() != null && item.getUnitCost() != null) {
                total = total.add(item.getUnitCost()
                        .multiply(java.math.BigDecimal.valueOf(item.getQuantityOrdered())));
            }
        }
        return total;
    }

    /** Units ordered across all lines. */
    public int totalQuantity() {
        if (items == null) return 0;
        return items.stream()
                .mapToInt(i -> i.getQuantityOrdered() == null ? 0 : i.getQuantityOrdered())
                .sum();
    }
}
