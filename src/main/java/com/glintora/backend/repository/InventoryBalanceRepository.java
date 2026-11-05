package com.glintora.backend.repository;

import com.glintora.backend.model.InventoryBalance;
import com.glintora.backend.model.InventoryBalanceKey;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.math.BigDecimal;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

@Repository
public interface InventoryBalanceRepository extends JpaRepository<InventoryBalance, InventoryBalanceKey> {

    List<InventoryBalance> findByVariantId(Long variantId);

    List<InventoryBalance> findByWarehouseId(Long warehouseId);

    /** Row-locked read used during checkout to make reservation concurrency-safe. */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select b from InventoryBalance b where b.variant.id = :variantId and b.warehouse.id = :warehouseId")
    Optional<InventoryBalance> findLocked(@Param("variantId") Long variantId,
                                          @Param("warehouseId") Long warehouseId);

    /** Sum of on-hand quantity across all warehouses for the given variants. */
    @Query("""
           select b.variant.id, coalesce(sum(b.quantityOnHand), 0)
           from InventoryBalance b
           where b.variant.id in :ids
           group by b.variant.id
           """)
    List<Object[]> sumOnHandByVariant(@Param("ids") Collection<Long> variantIds);

    // ------------------------------------------------------ valuation/health

    /** Total on-hand units across every warehouse. */
    @Query("select coalesce(sum(b.quantityOnHand), 0) from InventoryBalance b")
    long totalOnHandUnits();

    /** Stock valued at cost price (valuation basis: cost). */
    @Query("""
           select coalesce(sum(b.quantityOnHand * coalesce(b.variant.product.costPrice, 0)), 0)
           from InventoryBalance b
           """)
    BigDecimal totalInventoryValueAtCost();

    /** [variantId, onHand] for every variant that has a balance row. */
    @Query("select b.variant.id, coalesce(sum(b.quantityOnHand), 0) from InventoryBalance b group by b.variant.id")
    List<Object[]> onHandAllVariants();

    /** [warehouseId, distinct variant count, total on-hand units] per warehouse. */
    @Query("""
           select b.warehouse.id, count(distinct b.variant.id), coalesce(sum(b.quantityOnHand), 0)
           from InventoryBalance b
           group by b.warehouse.id
           """)
    List<Object[]> warehouseStockSummary();

    /** [warehouseId, stock valued at cost] per warehouse. */
    @Query("""
           select b.warehouse.id,
                  coalesce(sum(b.quantityOnHand * coalesce(b.variant.product.costPrice, 0)), 0)
           from InventoryBalance b
           group by b.warehouse.id
           """)
    List<Object[]> valueByWarehouse();
}
