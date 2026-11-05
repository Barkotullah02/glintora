package com.glintora.backend.model;

import jakarta.persistence.*;
import lombok.*;

/**
 * Every sale, regardless of channel (Website, Facebook, WhatsApp, Retail),
 * becomes an order in the same system so inventory and reporting stay
 * consistent.
 */
@Entity
@Table(name = "sales_channels")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SalesChannel {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String name;
}
