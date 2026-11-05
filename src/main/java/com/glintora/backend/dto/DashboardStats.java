package com.glintora.backend.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.Objects;

/** Aggregated numbers for the admin dashboard (guide §4). */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DashboardStats {

    // ---- KPI cards (selected period, with previous-period comparison) ----
    private int periodDays;
    private BigDecimal revenue;             // revenue in the selected period
    private BigDecimal previousRevenue;     // immediately preceding equal period
    private long orderCount;                // orders in period
    private long previousOrderCount;
    private BigDecimal inventoryValue;      // current stock at cost
    private long lowStockCount;
    private long outOfStockCount;

    // ---- catalogue / customer context ----
    private long totalProducts;
    private long publishedProducts;
    private long draftProducts;
    private long totalCustomers;

    // ---- inventory health (current state, independent of period) ----
    private long inStockVariants;
    private long reservedUnits;
    private long incomingUnits;             // outstanding PO quantity

    // ---- needs your attention ----
    private long pendingOrders;
    private long openPurchaseOrders;
    private long overdueShipments;
    private long pendingReviews;

    // ---- supporting data ----
    private List<LowStockLine> lowStockLines;
    private List<RevenuePoint> chart;       // one bucket per day in period

    /** Percentage change vs the previous period, or null when not meaningful. */
    public BigDecimal revenueChangePct() {
        if (previousRevenue == null || previousRevenue.signum() == 0) {
            return revenue != null && revenue.signum() > 0 ? BigDecimal.valueOf(100) : null;
        }
        return revenue.subtract(previousRevenue)
                .multiply(BigDecimal.valueOf(100))
                .divide(previousRevenue, 1, RoundingMode.HALF_UP);
    }

    public BigDecimal orderChangePct() {
        if (previousOrderCount == 0) {
            return orderCount > 0 ? BigDecimal.valueOf(100) : null;
        }
        return BigDecimal.valueOf(orderCount - previousOrderCount)
                .multiply(BigDecimal.valueOf(100))
                .divide(BigDecimal.valueOf(previousOrderCount), 1, RoundingMode.HALF_UP);
    }

    /** Tallest bucket in the chart, never below 1 so bars can't divide by zero. */
    public BigDecimal chartMax() {
        if (chart == null || chart.isEmpty()) {
            return BigDecimal.ONE;
        }
        return chart.stream()
                .map(RevenuePoint::getRevenue)
                .filter(Objects::nonNull)
                .max(BigDecimal::compareTo)
                .filter(m -> m.signum() > 0)
                .orElse(BigDecimal.ONE);
    }

    /** Bar height as a percentage of the tallest bucket (0–100). */
    public int barHeight(RevenuePoint point) {
        if (point == null || point.getRevenue() == null || point.getRevenue().signum() <= 0) {
            return 0;
        }
        return point.getRevenue()
                .multiply(BigDecimal.valueOf(100))
                .divide(chartMax(), 0, RoundingMode.HALF_UP)
                .intValue();
    }

    /** One day of the sales chart. */
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class RevenuePoint {
        private String day;        // 'dd MMM'
        private BigDecimal revenue;
        private long orders;

        /** Tooltip text for one bar. */
        public String tip() {
            return day + ": ৳" + revenue + " · " + orders + " order" + (orders == 1 ? "" : "s");
        }
    }
}
