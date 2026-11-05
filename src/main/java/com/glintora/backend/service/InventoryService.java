package com.glintora.backend.service;

import com.glintora.backend.model.*;
import com.glintora.backend.model.enums.MovementType;
import com.glintora.backend.model.enums.ReservationStatus;
import com.glintora.backend.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDateTime;
import java.util.*;

/**
 * Movement-based inventory service. Every stock change is posted as an
 * {@link InventoryMovement} and the matching {@link InventoryBalance} is
 * updated in the same transaction, keeping the audit history and the
 * fast-read summary consistent.
 *
 * <p>Key formulas:
 * <pre>
 * quantity_on_hand  = sum of movements
 * available_to_sell = quantity_on_hand - active reserved quantity
 * </pre>
 */
@Service
@RequiredArgsConstructor
public class InventoryService {

    private final InventoryMovementRepository movementRepo;
    private final InventoryBalanceRepository balanceRepo;
    private final StockReservationRepository reservationRepo;
    private final WarehouseRepository warehouseRepo;
    private final GoodsReceiptRepository goodsReceiptRepo;
    private final ProductVariantRepository variantRepo;
    private final InventoryReorderRuleRepository reorderRuleRepo;

    /** The warehouse used when no specific warehouse is chosen. */
    public Warehouse defaultWarehouse() {
        return warehouseRepo.findFirstByOrderByIdAsc()
                .orElseThrow(() -> new InvalidOperationException("No warehouse configured. Run the seed migration."));
    }

    // ---------------------------------------------------------------- reads

    @Transactional(readOnly = true)
    public Map<Long, Integer> onHandByVariant(Collection<Long> variantIds) {
        return toMap(balanceRepo.sumOnHandByVariant(variantIds));
    }

    @Transactional(readOnly = true)
    public Map<Long, Integer> reservedByVariant(Collection<Long> variantIds) {
        return toMap(reservationRepo.sumReservedByVariant(variantIds, ReservationStatus.active));
    }

    @Transactional(readOnly = true)
    public Map<Long, Integer> availableByVariant(Collection<Long> variantIds) {
        Map<Long, Integer> onHand = onHandByVariant(variantIds);
        Map<Long, Integer> reserved = reservedByVariant(variantIds);
        Map<Long, Integer> available = new HashMap<>();
        for (Long id : variantIds) {
            available.put(id, onHand.getOrDefault(id, 0) - reserved.getOrDefault(id, 0));
        }
        return available;
    }

    @Transactional(readOnly = true)
    public int onHand(Long variantId) {
        return onHandByVariant(List.of(variantId)).getOrDefault(variantId, 0);
    }

    @Transactional(readOnly = true)
    public int available(Long variantId) {
        return availableByVariant(List.of(variantId)).getOrDefault(variantId, 0);
    }

    @Transactional(readOnly = true)
    public int reserved(Long variantId) {
        return reservedByVariant(List.of(variantId)).getOrDefault(variantId, 0);
    }

    /**
     * Guide §6 stock overview: one row per (variant, warehouse) balance with
     * on hand / reserved / available and an in-low-out status derived from
     * the variant's reorder rule. Optional filters narrow the list.
     */
    @Transactional(readOnly = true)
    public List<com.glintora.backend.dto.StockRow> stockOverview(Long warehouseId, String status, String q) {
        List<InventoryBalance> balances = warehouseId == null
                ? balanceRepo.findAll()
                : balanceRepo.findByWarehouseId(warehouseId);

        // Reserved quantity per (variant, warehouse)
        Map<String, Integer> reservedAt = new HashMap<>();
        for (Object[] row : reservationRepo.reservedByVariantAndWarehouse(ReservationStatus.active)) {
            reservedAt.put(row[0] + ":" + row[1], ((Number) row[2]).intValue());
        }

        // Reorder rule per (variant, warehouse)
        Map<String, InventoryReorderRule> rules = new HashMap<>();
        for (InventoryReorderRule rule : reorderRuleRepo.findAll()) {
            rules.put(rule.getVariant().getId() + ":" + rule.getWarehouse().getId(), rule);
        }

        String needle = q == null || q.isBlank() ? null : q.trim().toLowerCase();
        List<com.glintora.backend.dto.StockRow> rows = new ArrayList<>();
        for (InventoryBalance balance : balances) {
            ProductVariant variant = balance.getVariant();
            if (needle != null) {
                String haystack = (variant.getSku() + " "
                        + (variant.getProduct() != null ? variant.getProduct().getName() : "")).toLowerCase();
                if (!haystack.contains(needle)) {
                    continue;
                }
            }
            int onHand = balance.getQuantityOnHand() == null ? 0 : balance.getQuantityOnHand();
            int reserved = reservedAt.getOrDefault(variant.getId() + ":" + balance.getWarehouse().getId(), 0);
            int available = Math.max(0, onHand - reserved);

            InventoryReorderRule rule = rules.get(variant.getId() + ":" + balance.getWarehouse().getId());
            boolean reorderRequired = rule != null && available <= rule.getReorderPoint();
            String rowStatus = available <= 0 ? "out" : (reorderRequired ? "low" : "in");

            if (status != null && !status.isBlank() && !status.equals(rowStatus)) {
                continue;
            }
            rows.add(new com.glintora.backend.dto.StockRow(
                    variant.getId(),
                    variant.getSku(),
                    variant.getProduct() != null ? variant.getProduct().getName() : "-",
                    variant.getSize() != null ? variant.getSize().getSizeName() : "-",
                    variant.getColor() != null ? variant.getColor().getColorName() : "-",
                    balance.getWarehouse().getId(),
                    balance.getWarehouse().getName(),
                    onHand,
                    reserved,
                    available,
                    rowStatus,
                    reorderRequired));
        }
        rows.sort(Comparator.comparing(com.glintora.backend.dto.StockRow::getProductName)
                .thenComparing(com.glintora.backend.dto.StockRow::getSku));
        return rows;
    }

    /**
     * Row-locked read of one balance, used by checkout to serialize
     * concurrent reservations of the same (variant, warehouse) pair.
     */
    @Transactional
    public Optional<InventoryBalance> lockBalance(Long variantId, Long warehouseId) {
        return balanceRepo.findLocked(variantId, warehouseId);
    }

    // ------------------------------------------------------------- movements

    /**
     * Post one signed inventory movement and update the balance in the same
     * transaction. The balance row is pessimistically locked so concurrent
     * postings cannot interleave.
     */
    @Transactional
    public InventoryMovement postMovement(Long variantId, Long warehouseId, MovementType type,
                                          int signedQuantity, String referenceType, Long referenceId) {
        return postMovement(variantId, warehouseId, type, signedQuantity, referenceType, referenceId, null, null);
    }

    /** Full-form posting that also records who posted it and why (guide §6 audit trail). */
    @Transactional
    public InventoryMovement postMovement(Long variantId, Long warehouseId, MovementType type,
                                          int signedQuantity, String referenceType, Long referenceId,
                                          String actor, String reason) {
        ProductVariant variant = variantRepo.findById(variantId)
                .orElseThrow(() -> new InvalidOperationException("Unknown variant id: " + variantId));
        Warehouse warehouse = warehouseRepo.findById(warehouseId)
                .orElseThrow(() -> new InvalidOperationException("Unknown warehouse id: " + warehouseId));

        InventoryBalance balance = balanceRepo.findLocked(variantId, warehouseId)
                .orElseGet(() -> InventoryBalance.builder()
                        .variant(variant)
                        .warehouse(warehouse)
                        .quantityOnHand(0)
                        .build());

        int newOnHand = balance.getQuantityOnHand() + signedQuantity;
        if (newOnHand < 0) {
            throw new InsufficientStockException(
                    "Stock for " + variant.getSku() + " would go negative (on hand: "
                            + balance.getQuantityOnHand() + ", change: " + signedQuantity + ")");
        }
        balance.setQuantityOnHand(newOnHand);
        balance.setUpdatedAt(LocalDateTime.now());
        balanceRepo.save(balance);

        return movementRepo.save(InventoryMovement.builder()
                .variant(variant)
                .warehouse(warehouse)
                .movementType(type)
                .quantity(signedQuantity)
                .referenceType(referenceType)
                .referenceId(referenceId)
                .actor(actor)
                .reason(reason)
                .build());
    }

    // ------------------------------------------------------------ receiving

    /** One line of a goods receipt: received and (optionally) rejected quantities. */
    public record ReceiveLine(int received, int rejected) {}

    /**
     * Record goods physically received into a warehouse. Only the accepted
     * amount (received - rejected) is added to stock via receipt movements.
     *
     * @return the persisted goods receipt
     */
    @Transactional
    public GoodsReceipt receiveGoods(Long warehouseId, String receivedBy,
                                     Map<Long, ReceiveLine> lines, Long shipmentId) {
        if (lines == null || lines.isEmpty()) {
            throw new InvalidOperationException("A goods receipt needs at least one variant line.");
        }
        Warehouse warehouse = warehouseRepo.findById(warehouseId)
                .orElseThrow(() -> new InvalidOperationException("Unknown warehouse id: " + warehouseId));

        GoodsReceipt receipt = GoodsReceipt.builder()
                .warehouse(warehouse)
                .receivedBy(receivedBy)
                .build();

        // Validate and attach items first, then persist the receipt once so the
        // movements can reference the receipt's id.
        Map<Long, Integer> acceptedByVariant = new LinkedHashMap<>();
        for (Map.Entry<Long, ReceiveLine> entry : lines.entrySet()) {
            ProductVariant variant = variantRepo.findById(entry.getKey())
                    .orElseThrow(() -> new InvalidOperationException("Unknown variant id: " + entry.getKey()));
            ReceiveLine line = entry.getValue();
            if (line.received() <= 0) {
                throw new InvalidOperationException("Received quantity must be positive for " + variant.getSku());
            }
            int accepted = line.received() - line.rejected();
            if (accepted < 0) {
                throw new InvalidOperationException("Rejected quantity cannot exceed received quantity for " + variant.getSku());
            }
            receipt.getItems().add(GoodsReceiptItem.builder()
                    .goodsReceipt(receipt)
                    .variant(variant)
                    .quantityReceived(line.received())
                    .quantityRejected(line.rejected())
                    .build());
            acceptedByVariant.put(variant.getId(), accepted);
        }

        receipt = goodsReceiptRepo.save(receipt);

        for (Map.Entry<Long, Integer> entry : acceptedByVariant.entrySet()) {
            if (entry.getValue() > 0) {
                postMovement(entry.getKey(), warehouse.getId(), MovementType.receipt,
                        entry.getValue(), "goods_receipt", receipt.getId(),
                        receivedBy, "Goods receipt #" + receipt.getId());
            }
        }
        return receipt;
    }

    // ------------------------------------------------------------ adjustment

    /** Manual stock adjustment (damage write-off, count correction, returns, etc.). */
    @Transactional
    public void adjust(Long variantId, Long warehouseId, int delta, MovementType type, String reason) {
        adjust(variantId, warehouseId, delta, type, reason, "admin");
    }

    /** Manual adjustment that also records who made it. */
    @Transactional
    public void adjust(Long variantId, Long warehouseId, int delta, MovementType type, String reason, String actor) {
        if (delta == 0) {
            return;
        }
        if (type == null) {
            type = delta > 0 ? MovementType.adjustment_in : MovementType.adjustment_out;
        }
        boolean isInType = type == MovementType.adjustment_in || type == MovementType.customer_return;
        if (isInType != (delta > 0)) {
            throw new InvalidOperationException(
                    "Movement type '" + type + "' does not match the sign of the quantity change.");
        }
        postMovement(variantId, warehouseId, type, delta, "adjustment", null,
                actor == null || actor.isBlank() ? "admin" : actor,
                reason == null || reason.isBlank() ? "Manual adjustment" : reason);
    }

    // ------------------------------------------------- order dispatch/release

    /**
     * Physically dispatch an order: convert each active reservation into a
     * sale_dispatch movement and complete the reservation in one transaction.
     */
    @Transactional
    public void dispatchOrderItems(Order order) {
        for (OrderItem item : order.getItems()) {
            StockReservation reservation = reservationRepo
                    .findFirstByOrderItemIdAndStatus(item.getId(), ReservationStatus.active)
                    .orElse(null);

            if (reservation != null) {
                postMovement(item.getVariant().getId(), reservation.getWarehouse().getId(),
                        MovementType.sale_dispatch, -item.getQuantity(), "order", order.getId(),
                        "system", "Order #" + order.getId() + " dispatch");
                reservation.setStatus(ReservationStatus.fulfilled);
                reservationRepo.save(reservation);
            } else {
                // No reservation (legacy/manually created order): dispatch from the default warehouse.
                postMovement(item.getVariant().getId(), defaultWarehouse().getId(),
                        MovementType.sale_dispatch, -item.getQuantity(), "order", order.getId(),
                        "system", "Order #" + order.getId() + " dispatch");
            }
        }
    }

    /** Release all active reservations held by an order (cancellation/expiry). */
    @Transactional
    public void releaseReservations(Order order) {
        for (OrderItem item : order.getItems()) {
            reservationRepo.findByOrderItemId(item.getId()).stream()
                    .filter(r -> r.getStatus() == ReservationStatus.active)
                    .forEach(r -> {
                        r.setStatus(ReservationStatus.released);
                        reservationRepo.save(r);
                    });
        }
    }

    // ------------------------------------------------------------- reorder

    @Transactional(readOnly = true)
    public List<InventoryReorderRule> allReorderRules() {
        return reorderRuleRepo.findAllByOrderByVariantIdAsc();
    }

    private static Map<Long, Integer> toMap(List<Object[]> rows) {
        Map<Long, Integer> map = new HashMap<>();
        for (Object[] row : rows) {
            map.put((Long) row[0], ((Number) row[1]).intValue());
        }
        return map;
    }
}
