package com.glintora.backend.model;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "customer_addresses")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CustomerAddress {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "customer_id", nullable = false)
    private Customer customer;

    /** delivery or billing */
    @Column(name = "address_type", nullable = false)
    @Builder.Default
    private String addressType = "delivery";

    @Column(nullable = false, columnDefinition = "TEXT")
    private String address;

    @Column(nullable = false)
    private String city;

    @Column(name = "postal_code")
    private String postalCode;

    @Column(name = "is_default")
    @Builder.Default
    private Boolean isDefault = false;
}
