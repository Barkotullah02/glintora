package com.glintora.backend.repository;

import com.glintora.backend.model.ProductAttribute;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface ProductAttributeRepository extends JpaRepository<ProductAttribute, Long> {
    List<ProductAttribute> findByProductId(Long productId);
}
