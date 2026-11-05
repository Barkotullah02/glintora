package com.glintora.backend.repository;

import com.glintora.backend.model.ProductMeasurement;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface ProductMeasurementRepository extends JpaRepository<ProductMeasurement, Long> {
    List<ProductMeasurement> findByProductIdOrderByGarmentSectionAscMeasurementNameAsc(Long productId);
}
