package com.glintora.backend.repository;

import com.glintora.backend.model.StockReservation;
import com.glintora.backend.model.enums.ReservationStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

@Repository
public interface StockReservationRepository extends JpaRepository<StockReservation, Long> {

    Optional<StockReservation> findFirstByOrderItemIdAndStatus(Long orderItemId, ReservationStatus status);

    List<StockReservation> findByOrderItemId(Long orderItemId);

    List<StockReservation> findByStatus(ReservationStatus status);

    /** Sum of active reserved quantity for the given variants. */
    @Query("""
           select r.variant.id, coalesce(sum(r.quantity), 0)
           from StockReservation r
           where r.status = :status and r.variant.id in :ids
           group by r.variant.id
           """)
    List<Object[]> sumReservedByVariant(@Param("ids") Collection<Long> variantIds,
                                        @Param("status") ReservationStatus status);

    /** Total units currently reserved across all variants. */
    @Query("select coalesce(sum(r.quantity), 0) from StockReservation r where r.status = :status")
    long sumAllReserved(@Param("status") ReservationStatus status);

    /** [variantId, warehouseId, reserved] per location. */
    @Query("""
           select r.variant.id, r.warehouse.id, coalesce(sum(r.quantity), 0)
           from StockReservation r
           where r.status = :status
           group by r.variant.id, r.warehouse.id
           """)
    List<Object[]> reservedByVariantAndWarehouse(@Param("status") ReservationStatus status);
}
