package com.glintora.backend.repository;

import com.glintora.backend.model.ProductVariant;
import com.glintora.backend.model.enums.ProductVariantStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;

@Repository
public interface ProductVariantRepository extends JpaRepository<ProductVariant, Long> {
    List<ProductVariant> findByProductIdOrderBySkuAsc(Long productId);
    Optional<ProductVariant> findBySku(String sku);
    List<ProductVariant> findByStatus(ProductVariantStatus status);
}
