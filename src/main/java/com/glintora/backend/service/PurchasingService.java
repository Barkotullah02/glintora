package com.glintora.backend.service;

import com.glintora.backend.dto.LowStockLine;
import com.glintora.backend.model.*;
import com.glintora.backend.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.*;

/**
 * Purchasing workflow (guide §7): purchase orders, incoming shipments and
 * the reorder-planning bridge that turns a suggestion into a *draft* PO.
 *
 * <p>A purchase order never increases sellable stock — stock only moves
 * when goods are physically received against it.
 */
@Service
@RequiredArgsConstructor
public class PurchasingService {

    /** PO states we allow, with their legal successors. */
    private static final Map<String, List<String>> PO_TRANSITIONS = Map.of(
            "draft", List.of("ordered", "cancelled"),
            "ordered", List.of("partial", "received", "cancelled"),
            "partial", List.of("received", "cancelled"),
            "received", List.of(),
            "cancelled", List.of());

    /** Guide §7 shipment timeline: Ordered → … → Completed. */
    private static final Map<String, List<String>> SHIPMENT_TRANSITIONS = Map.of(
            "ordered", List.of("dispatched", "cancelled"),
            "dispatched", List.of("in_transit", "cancelled"),
            "in_transit", List.of("arrived", "cancelled"),
            "arrived", List.of("receiving", "cancelled"),
            "receiving", List.of("completed", "arrived"),
            "completed", List.of(),
            "cancelled", List.of());

    private final PurchaseOrderRepository poRepo;
    private final SupplierRepository supplierRepo;
    private final ShipmentRepository shipmentRepo;
    private final ProductVariantRepository variantRepo;
    private final GoodsReceiptRepository goodsReceiptRepo;

    // ------------------------------------------------------- purchase orders

    @Transactional(readOnly = true)
    public List<PurchaseOrder> allPurchaseOrders() {
        return poRepo.findAllByOrderByOrderDateDesc();
    }

    @Transactional(readOnly = true)
    public PurchaseOrder getPo(Long id) {
        return poRepo.findById(id)
                .orElseThrow(() -> new InvalidOperationException("Purchase order not found: " + id));
    }

    /** Move a PO to its next state; rejects illegal jumps server-side. */
    @Transactional
    public PurchaseOrder changePoStatus(Long poId, String target) {
        PurchaseOrder po = getPo(poId);
        List<String> allowed = PO_TRANSITIONS.getOrDefault(po.getStatus(), List.of());
        if (!allowed.contains(target)) {
            throw new InvalidOperationException(
                    "A " + po.getStatus() + " purchase order cannot become " + target + ".");
        }
        po.setStatus(target);
        return poRepo.save(po);
    }

    /**
     * Create a draft PO for a supplier with the given lines. Lines are
     * {@code variantId -> [quantity, unitCost]} pairs supplied by the form.
     */
    @Transactional
    public PurchaseOrder createDraftPo(Long supplierId, LocalDate expectedDate,
                                       List<Long> variantIds, List<Integer> quantities,
                                       List<BigDecimal> unitCosts) {
        Supplier supplier = supplierRepo.findById(supplierId)
                .orElseThrow(() -> new InvalidOperationException("Unknown supplier."));
        if (variantIds == null || variantIds.isEmpty()) {
            throw new InvalidOperationException("A purchase order needs at least one line.");
        }
        PurchaseOrder po = PurchaseOrder.builder()
                .supplier(supplier)
                .orderDate(LocalDate.now())
                .expectedDate(expectedDate)
                .status("draft")
                .build();
        po = poRepo.save(po);

        for (int i = 0; i < variantIds.size(); i++) {
            if (variantIds.get(i) == null || quantities.get(i) == null || quantities.get(i) <= 0) {
                continue; // blank form row
            }
            ProductVariant variant = variantRepo.findById(variantIds.get(i))
                    .orElseThrow(() -> new InvalidOperationException("Unknown variant on PO line."));
            BigDecimal cost = unitCosts != null && i < unitCosts.size() && unitCosts.get(i) != null
                    ? unitCosts.get(i) : BigDecimal.ZERO;
            po.getItems().add(PurchaseOrderItem.builder()
                    .purchaseOrder(po)
                    .variant(variant)
                    .quantityOrdered(quantities.get(i))
                    .unitCost(cost)
                    .build());
        }
        if (po.getItems().isEmpty()) {
            throw new InvalidOperationException("A purchase order needs at least one line with a quantity.");
        }
        return poRepo.save(po);
    }

    /**
     * Reorder planning (guide §6): turn one suggestion into a draft PO.
     * Suggestions create drafts only — nothing is ordered automatically.
     */
    @Transactional
    public PurchaseOrder createDraftFromSuggestion(LowStockLine line, Long supplierId, Integer quantity) {
        int qty = quantity == null || quantity <= 0 ? line.getSuggestedReorderQty() : quantity;
        if (qty <= 0) {
            throw new InvalidOperationException("Suggested reorder quantity is zero for " + line.getSku() + ".");
        }
        ProductVariant variant = variantRepo.findById(line.getVariantId())
                .orElseThrow(() -> new InvalidOperationException("Unknown variant."));
        Supplier supplier = supplierRepo.findById(supplierId)
                .orElseThrow(() -> new InvalidOperationException("Choose a supplier for the draft PO."));

        BigDecimal cost = variant.getProduct() != null && variant.getProduct().getCostPrice() != null
                ? variant.getProduct().getCostPrice() : BigDecimal.ZERO;

        PurchaseOrder po = PurchaseOrder.builder()
                .supplier(supplier)
                .orderDate(LocalDate.now())
                .expectedDate(LocalDate.now().plusDays(14))
                .status("draft")
                .build();
        po = poRepo.save(po);
        po.getItems().add(PurchaseOrderItem.builder()
                .purchaseOrder(po)
                .variant(variant)
                .quantityOrdered(qty)
                .unitCost(cost)
                .build());
        return poRepo.save(po);
    }

    // ------------------------------------------------------------- shipments

    @Transactional(readOnly = true)
    public List<Shipment> allShipments() {
        return shipmentRepo.findAllByOrderByExpectedArrivalDateAsc();
    }

    @Transactional(readOnly = true)
    public Shipment getShipment(Long id) {
        return shipmentRepo.findById(id)
                .orElseThrow(() -> new InvalidOperationException("Shipment not found: " + id));
    }

    @Transactional
    public Shipment changeShipmentStatus(Long shipmentId, String target) {
        Shipment shipment = getShipment(shipmentId);
        List<String> allowed = SHIPMENT_TRANSITIONS.getOrDefault(shipment.getStatus(), List.of());
        if (!allowed.contains(target)) {
            throw new InvalidOperationException(
                    "A " + shipment.getStatus() + " shipment cannot become " + target + ".");
        }
        shipment.setStatus(target);
        return shipmentRepo.save(shipment);
    }

    /** Book an incoming shipment against an open purchase order. */
    @Transactional
    public Shipment createShipment(Long poId, String reference, LocalDate dispatchDate,
                                   LocalDate expectedArrival) {
        PurchaseOrder po = getPo(poId);
        if ("received".equals(po.getStatus()) || "cancelled".equals(po.getStatus())) {
            throw new InvalidOperationException("Cannot ship a " + po.getStatus() + " purchase order.");
        }
        Shipment shipment = Shipment.builder()
                .purchaseOrder(po)
                .shipmentReference(reference)
                .dispatchDate(dispatchDate)
                .expectedArrivalDate(expectedArrival)
                .status("ordered")
                .build();
        return shipmentRepo.save(shipment);
    }

    // ------------------------------------------------------- goods receiving

    /**
     * Guide §7 goods receiving: per-variant ordered / previously received /
     * remaining quantities for one PO, so staff can receive partially.
     */
    @Transactional(readOnly = true)
    public List<ReceivingLine> receivingLines(Long poId) {
        PurchaseOrder po = getPo(poId);
        Map<Long, Integer> received = receivedByVariant(poId);
        List<ReceivingLine> lines = new ArrayList<>();
        for (PurchaseOrderItem item : po.getItems()) {
            int already = received.getOrDefault(item.getVariant().getId(), 0);
            lines.add(new ReceivingLine(
                    item.getVariant().getId(),
                    item.getVariant().getSku(),
                    item.getVariant().getProduct() != null ? item.getVariant().getProduct().getName() : "-",
                    item.getQuantityOrdered(),
                    already,
                    Math.max(0, item.getQuantityOrdered() - already)));
        }
        return lines;
    }

    /** variantId → accepted quantity already received against this PO. */
    private Map<Long, Integer> receivedByVariant(Long poId) {
        Map<Long, Integer> received = new HashMap<>();
        for (Shipment shipment : shipmentRepo.findByPurchaseOrderId(poId)) {
            for (GoodsReceipt receipt : goodsReceiptsOfShipment(shipment.getId())) {
                for (GoodsReceiptItem item : receipt.getItems()) {
                    received.merge(item.getVariant().getId(), item.quantityAccepted(), Integer::sum);
                }
            }
        }
        return received;
    }

    @Transactional(readOnly = true)
    public List<GoodsReceipt> goodsReceiptsOfShipment(Long shipmentId) {
        return goodsReceiptRepo.findByShipmentId(shipmentId);
    }

    /** A receiving line for the goods-receiving screen. */
    public record ReceivingLine(Long variantId, String sku, String productName,
                                int ordered, int received, int remaining) {}
}
