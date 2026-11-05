package com.glintora.backend.model;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "colors")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Color {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "color_name", nullable = false, unique = true)
    private String colorName;

    @Column(name = "hex_code")
    private String hexCode;
}
