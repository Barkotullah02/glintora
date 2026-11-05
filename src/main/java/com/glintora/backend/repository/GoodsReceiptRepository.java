package com.glintora.backend.repository;

import com.glintora.backend.model.GoodsReceipt;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface GoodsReceiptRepository extends JpaRepository<GoodsReceipt, Long> {
    List<GoodsReceipt> findAllByOrderByReceivedAtDesc();

    List<GoodsReceipt> findByShipmentId(Long shipmentId);
}
