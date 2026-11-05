package com.glintora.backend.repository;

import com.glintora.backend.model.Shipment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.time.LocalDate;
import java.util.List;

@Repository
public interface ShipmentRepository extends JpaRepository<Shipment, Long> {
    List<Shipment> findByPurchaseOrderId(Long purchaseOrderId);

    List<Shipment> findAllByOrderByExpectedArrivalDateAsc();

    /** Shipments past their expected arrival that have not landed yet. */
    @Query("""
           select s from Shipment s
           where s.expectedArrivalDate < :today
             and s.status in ('dispatched', 'in_transit', 'ordered')
           order by s.expectedArrivalDate asc
           """)
    List<Shipment> findOverdue(@Param("today") LocalDate today);
}
