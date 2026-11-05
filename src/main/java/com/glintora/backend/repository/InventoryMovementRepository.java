package com.glintora.backend.repository;

import com.glintora.backend.model.InventoryMovement;
import com.glintora.backend.model.enums.MovementType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface InventoryMovementRepository extends JpaRepository<InventoryMovement, Long> {
    List<InventoryMovement> findTop50ByOrderByCreatedAtDesc();
    List<InventoryMovement> findByVariantIdOrderByCreatedAtDesc(Long variantId);

    List<InventoryMovement> findTop200ByOrderByCreatedAtDesc();

    /** Movement log filtered by type and/or warehouse; newest first. */
    @Query("""
           select m from InventoryMovement m
           where (:type is null or m.movementType = :type)
             and (:warehouseId is null or m.warehouse.id = :warehouseId)
           order by m.createdAt desc
           """)
    List<InventoryMovement> search(@Param("type") MovementType type,
                                   @Param("warehouseId") Long warehouseId);
}
