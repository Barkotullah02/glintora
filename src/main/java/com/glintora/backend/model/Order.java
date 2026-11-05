package com.glintora.backend.model;

import com.glintora.backend.model.enums.OrderStatus;
import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "orders")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Order {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "customer_id", nullable = false)
    private Customer customer;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "sales_channel_id", nullable = false)
    private SalesChannel salesChannel;

    @Column(name = "order_date", updatable = false)
    @Builder.Default
    private LocalDateTime orderDate = LocalDateTime.now();

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Builder.Default
    private OrderStatus status = OrderStatus.pending;

    @Column(nullable = false)
    private BigDecimal subtotal;

    @Column(name = "discount_total", nullable = false)
    @Builder.Default
    private BigDecimal discountTotal = BigDecimal.ZERO;

    @Column(name = "shipping_fee", nullable = false)
    @Builder.Default
    private BigDecimal shippingFee = BigDecimal.ZERO;

    @Column(name = "grand_total", nullable = false)
    private BigDecimal grandTotal;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "shipping_address_id")
    private CustomerAddress shippingAddress;

    @OneToMany(mappedBy = "order", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    @OrderBy("id ASC")
    private List<OrderItem> items = new ArrayList<>();

    @OneToMany(mappedBy = "order", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<Payment> payments = new ArrayList<>();

    @OneToMany(mappedBy = "order", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<OrderStatusHistory> statusHistory = new ArrayList<>();

    @OneToMany(mappedBy = "order", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<DeliveryShipment> deliveryShipments = new ArrayList<>();

    /**
     * Derived payment state for lists/detail: paid, pending, refunded,
     * failed or unpaid (no payment record yet). Never stored — always
     * recomputed from the payment records so it cannot drift.
     */
    public String paymentStatusSummary() {
        if (payments == null || payments.isEmpty()) {
            return "unpaid";
        }
        boolean paid = false, pending = false, refunded = false, failed = false;
        for (Payment p : payments) {
            switch (p.getStatus()) {
                case paid -> paid = true;
                case pending -> pending = true;
                case refunded -> refunded = true;
                case failed -> failed = true;
            }
        }
        if (paid) return "paid";
        if (refunded) return "refunded";
        if (pending) return "pending";
        if (failed) return "failed";
        return "unpaid";
    }

    /** Fulfillment view of the order status for the admin order list. */
    public String fulfillmentStatusSummary() {
        if (status == null) return "-";
        return switch (status) {
            case pending -> "awaiting confirmation";
            case confirmed -> "ready to process";
            case processing -> "processing";
            case shipped -> "shipped";
            case delivered -> "delivered";
            case cancelled -> "cancelled";
            case returned -> "returned";
        };
    }
}
