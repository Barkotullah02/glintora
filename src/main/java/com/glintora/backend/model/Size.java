package com.glintora.backend.model;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "sizes")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Size {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "size_name", nullable = false, unique = true)
    private String sizeName;
}
