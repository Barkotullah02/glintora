package com.glintora.backend.repository;

import com.glintora.backend.model.PurchaseOrderItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface PurchaseOrderItemRepository extends JpaRepository<PurchaseOrderItem, Long> {
    List<PurchaseOrderItem> findByPurchaseOrderId(Long purchaseOrderId);

    /** Quantity ordered but not yet received, per variant (open POs only). */
    @Query("""
           select i.variant.id, coalesce(sum(i.quantityOrdered), 0)
           from PurchaseOrderItem i
           where i.purchaseOrder.status in ('draft', 'ordered', 'partial')
           group by i.variant.id
           """)
    List<Object[]> sumOutstandingByVariant();
}
