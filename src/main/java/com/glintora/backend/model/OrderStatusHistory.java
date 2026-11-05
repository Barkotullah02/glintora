package com.glintora.backend.model;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

/** Audit trail of order status transitions. */
@Entity
@Table(name = "order_status_history")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OrderStatusHistory {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "order_id", nullable = false)
    private Order order;

    @Column(name = "old_status", length = 50)
    private String oldStatus;

    @Column(name = "new_status", nullable = false, length = 50)
    private String newStatus;

    @Column(name = "changed_at", updatable = false)
    @Builder.Default
    private LocalDateTime changedAt = LocalDateTime.now();
}
