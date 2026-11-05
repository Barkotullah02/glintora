package com.glintora.backend.service;

import com.glintora.backend.dto.DashboardStats;
import com.glintora.backend.dto.DashboardStats.RevenuePoint;
import com.glintora.backend.dto.LowStockLine;
import com.glintora.backend.model.InventoryReorderRule;
import com.glintora.backend.model.Order;
import com.glintora.backend.model.ProductVariant;
import com.glintora.backend.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

/** Aggregates the operational numbers shown on the admin dashboard (guide §4). */
@Service
@RequiredArgsConstructor
public class DashboardService {

    private static final DateTimeFormatter DAY_LABEL = DateTimeFormatter.ofPattern("dd MMM");

    private final ProductRepository productRepo;
    private final OrderRepository orderRepo;
    private final CustomerRepository customerRepo;
    private final PurchaseOrderItemRepository purchaseItemRepo;
    private final PurchaseOrderRepository purchaseOrderRepo;
    private final ShipmentRepository shipmentRepo;
    private final ProductReviewRepository reviewRepo;
    private final InventoryBalanceRepository balanceRepo;
    private final StockReservationRepository reservationRepo;
    private final InventoryService inventoryService;

    /** KPI cards + chart + health + attention for the given reporting period. */
    @Transactional(readOnly = true)
    public DashboardStats stats(int periodDays) {
        int days = Math.max(1, Math.min(365, periodDays));
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime periodStart = now.minusDays(days);
        LocalDateTime previousStart = now.minusDays(2L * days);

        // KPI period figures
        BigDecimal revenue = orderRepo.sumRevenueBetween(periodStart, now);
        BigDecimal previousRevenue = orderRepo.sumRevenueBetween(previousStart, periodStart);
        long orderCount = orderRepo.countBetween(periodStart, now);
        long previousOrderCount = orderRepo.countBetween(previousStart, periodStart);

        // Inventory health — current state regardless of the sales period
        List<InventoryReorderRule> rules = inventoryService.allReorderRules();
        List<LowStockLine> lowStock = lowStockLines(rules);
        long outOfStock = lowStock.stream().filter(l -> l.getAvailable() <= 0).count();
        Set<Long> allVariantIds = allVariantIds();
        Map<Long, Integer> available = inventoryService.availableByVariant(allVariantIds);
        long inStockVariants = available.values().stream().filter(a -> a > 0).count();
        long reservedUnits = reservationRepo.sumAllReserved(
                com.glintora.backend.model.enums.ReservationStatus.active);
        long incomingUnits = purchaseItemRepo.sumOutstandingByVariant().stream()
                .mapToLong(r -> ((Number) r[1]).longValue()).sum();

        return DashboardStats.builder()
                .periodDays(days)
                .revenue(revenue)
                .previousRevenue(previousRevenue)
                .orderCount(orderCount)
                .previousOrderCount(previousOrderCount)
                .inventoryValue(balanceRepo.totalInventoryValueAtCost())
                .lowStockCount(lowStock.size() - outOfStock)
                .outOfStockCount(outOfStock)
                .totalProducts(productRepo.count())
                .publishedProducts(productRepo.countByStatus(
                        com.glintora.backend.model.enums.ProductStatus.published))
                .draftProducts(productRepo.countByStatus(
                        com.glintora.backend.model.enums.ProductStatus.draft))
                .totalCustomers(customerRepo.count())
                .inStockVariants(inStockVariants)
                .reservedUnits(reservedUnits)
                .incomingUnits(incomingUnits)
                .pendingOrders(orderRepo.countByStatus(
                        com.glintora.backend.model.enums.OrderStatus.pending))
                .openPurchaseOrders(purchaseOrderRepo.countByStatusIn(List.of("draft", "ordered", "partial")))
                .overdueShipments(shipmentRepo.findOverdue(LocalDate.now()).size())
                .pendingReviews(reviewRepo.countByStatus("pending"))
                .lowStockLines(lowStock)
                .chart(revenueChart(periodStart))
                .build();
    }

    @Transactional(readOnly = true)
    public List<Order> recentOrders() {
        return orderRepo.findTop8ByOrderByOrderDateDesc();
    }

    /** Reorder-planning rows: every variant at/below its reorder point. */
    @Transactional(readOnly = true)
    public List<LowStockLine> lowStockLines() {
        return lowStockLines(inventoryService.allReorderRules());
    }

    /** Days in the period with no sales still get a zero bucket, so the chart reads honestly. */
    private List<RevenuePoint> revenueChart(LocalDateTime periodStart) {
        Map<LocalDate, RevenuePoint> byDay = new HashMap<>();
        for (Object[] row : orderRepo.revenueByDaySince(periodStart)) {
            LocalDate day = row[0] instanceof java.sql.Date d ? d.toLocalDate()
                    : (LocalDate) row[0];
            byDay.put(day, new RevenuePoint(day.format(DAY_LABEL),
                    (BigDecimal) row[1], ((Number) row[2]).longValue()));
        }
        List<RevenuePoint> points = new ArrayList<>();
        LocalDate start = periodStart.toLocalDate();
        LocalDate end = LocalDate.now();
        for (LocalDate d = !start.isAfter(end) ? start : end; !d.isAfter(end); d = d.plusDays(1)) {
            points.add(byDay.getOrDefault(d,
                    new RevenuePoint(d.format(DAY_LABEL), BigDecimal.ZERO, 0)));
        }
        return points;
    }

    private Set<Long> allVariantIds() {
        return productRepo.findAll().stream()
                .flatMap(p -> p.getVariants().stream())
                .map(ProductVariant::getId)
                .collect(Collectors.toSet());
    }

    /**
     * Variants at or below their reorder point, with a simple suggestion:
     * suggested = max(0, target - (available + outstanding ordered)).
     */
    private List<LowStockLine> lowStockLines(List<InventoryReorderRule> rules) {
        if (rules.isEmpty()) {
            return List.of();
        }
        Set<Long> variantIds = rules.stream()
                .map(r -> r.getVariant().getId())
                .collect(Collectors.toSet());

        Map<Long, Integer> onHand = inventoryService.onHandByVariant(variantIds);
        Map<Long, Integer> available = inventoryService.availableByVariant(variantIds);
        Map<Long, Integer> outstanding = new HashMap<>();
        for (Object[] row : purchaseItemRepo.sumOutstandingByVariant()) {
            outstanding.put((Long) row[0], ((Number) row[1]).intValue());
        }

        List<LowStockLine> lines = new ArrayList<>();
        for (InventoryReorderRule rule : rules) {
            ProductVariant v = rule.getVariant();
            int avail = available.getOrDefault(v.getId(), 0);
            if (avail > rule.getReorderPoint()) {
                continue;
            }
            int outstandingQty = outstanding.getOrDefault(v.getId(), 0);
            int suggested = Math.max(0, rule.getTargetQuantity() - (avail + outstandingQty));
            lines.add(LowStockLine.builder()
                    .ruleId(rule.getId())
                    .variantId(v.getId())
                    .sku(v.getSku())
                    .productName(v.getProduct() != null ? v.getProduct().getName() : v.getSku())
                    .sizeName(v.getSize() != null ? v.getSize().getSizeName() : "-")
                    .colorName(v.getColor() != null ? v.getColor().getColorName() : "-")
                    .onHand(onHand.getOrDefault(v.getId(), 0))
                    .reserved(onHand.getOrDefault(v.getId(), 0) - avail)
                    .available(avail)
                    .outstandingOrdered(outstandingQty)
                    .reorderPoint(rule.getReorderPoint())
                    .targetQuantity(rule.getTargetQuantity())
                    .suggestedReorderQty(suggested)
                    .build());
        }
        return lines;
    }
}
