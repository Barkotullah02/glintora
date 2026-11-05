package com.glintora.backend.repository;

import com.glintora.backend.model.DeliveryShipment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface DeliveryShipmentRepository extends JpaRepository<DeliveryShipment, Long> {
    List<DeliveryShipment> findByOrderId(Long orderId);
}
