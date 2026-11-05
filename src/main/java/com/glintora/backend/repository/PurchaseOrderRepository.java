package com.glintora.backend.repository;

import com.glintora.backend.model.PurchaseOrder;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.Collection;
import java.util.List;

@Repository
public interface PurchaseOrderRepository extends JpaRepository<PurchaseOrder, Long> {
    List<PurchaseOrder> findAllByOrderByOrderDateDesc();

    long countByStatusIn(Collection<String> statuses);
}
