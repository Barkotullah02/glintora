package com.glintora.backend.repository;

import com.glintora.backend.model.ProductImage;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;

@Repository
public interface ProductImageRepository extends JpaRepository<ProductImage, Long> {
    List<ProductImage> findByProductIdOrderBySortOrderAsc(Long productId);
    Optional<ProductImage> findFirstByProductIdAndIsPrimaryTrue(Long productId);
}
