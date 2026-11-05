package com.glintora.backend.service;

import com.glintora.backend.dto.CartLine;
import com.glintora.backend.model.*;
import com.glintora.backend.model.enums.MovementType;
import com.glintora.backend.model.enums.OrderStatus;
import com.glintora.backend.model.enums.PaymentStatus;
import com.glintora.backend.model.enums.ReservationStatus;
import com.glintora.backend.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;

/**
 * Order lifecycle: checkout (reserve stock atomically), confirmation,
 * dispatch (physical stock-out), delivery (payment settled for COD) and
 * cancellation (reservations released).
 *
 * <p>Reservation vs dispatch: at checkout we only reserve, so the goods are
 * not yet deducted from on-hand stock. The sale_dispatch movement is posted
 * when the order physically leaves the warehouse.
 */
@Service
@RequiredArgsConstructor
public class OrderService {

    private final OrderRepository orderRepo;
    private final ProductVariantRepository variantRepo;
    private final CustomerAddressRepository addressRepo;
    private final SalesChannelRepository channelRepo;
    private final StockReservationRepository reservationRepo;
    private final InventoryService inventoryService;

    /** Fixed shipping fee for the website channel. */
    public static final BigDecimal SHIPPING_FEE = new BigDecimal("60.00");

    /**
     * Place an order from cart lines. Re-checks and reserves stock under a
     * pessimistic lock per (variant, warehouse) row so two customers cannot
     * buy the same last unit.
     *
     * @param lineQuantities variantId -> quantity taken from the session cart
     */
    @Transactional
    public Order placeOrder(Customer customer, Map<Long, Integer> lineQuantities,
                            String address, String city, String postalCode,
                            String paymentMethod) {
        if (lineQuantities == null || lineQuantities.isEmpty()) {
            throw new InvalidOperationException("Your cart is empty.");
        }
        Warehouse warehouse = inventoryService.defaultWarehouse();

        // 1. Resolve and validate every line, locking balances as we go
        List<ResolvedLine> resolved = new ArrayList<>();
        for (Map.Entry<Long, Integer> entry : lineQuantities.entrySet()) {
            ProductVariant variant = variantRepo.findById(entry.getKey())
                    .orElseThrow(() -> new InvalidOperationException("A cart item is no longer available."));
            int requested = entry.getValue();
            if (requested < 1) {
                continue;
            }

            InventoryBalance balance = lockBalanceOrThrow(variant, warehouse);
            int reservedAlready = inventoryService.reserved(variant.getId());
            int availableNow = balance.getQuantityOnHand() - reservedAlready;
            if (availableNow < requested) {
                throw new InsufficientStockException(availableNow <= 0
                        ? "'" + label(variant) + "' is out of stock."
                        : "Only " + availableNow + " left of '" + label(variant) + "'.");
            }
            resolved.add(new ResolvedLine(variant, requested, balance.getWarehouse()));
        }
        if (resolved.isEmpty()) {
            throw new InvalidOperationException("Your cart is empty.");
        }

        // 2. Compute totals from price snapshots
        BigDecimal subtotal = BigDecimal.ZERO;
        for (ResolvedLine line : resolved) {
            BigDecimal price = line.variant.effectiveSellingPrice();
            if (price == null) {
                throw new InvalidOperationException("Missing price for " + line.variant.getSku());
            }
            subtotal = subtotal.add(price.multiply(BigDecimal.valueOf(line.quantity)));
        }

        CustomerAddress shipping = saveAddress(customer, address, city, postalCode);
        SalesChannel channel = channelRepo.findByNameIgnoreCase("Website")
                .orElseGet(() -> channelRepo.save(SalesChannel.builder().name("Website").build()));

        Order order = Order.builder()
                .customer(customer)
                .salesChannel(channel)
                .subtotal(subtotal)
                .discountTotal(BigDecimal.ZERO)
                .shippingFee(SHIPPING_FEE)
                .grandTotal(subtotal.add(SHIPPING_FEE))
                .shippingAddress(shipping)
                .status(OrderStatus.pending)
                .build();
        order = orderRepo.save(order);

        // 3. Order items with price/cost snapshots
        for (ResolvedLine line : resolved) {
            Product product = line.variant.getProduct();
            BigDecimal cost = product.getCostPrice() != null ? product.getCostPrice() : BigDecimal.ZERO;
            OrderItem item = OrderItem.builder()
                    .order(order)
                    .variant(line.variant)
                    .quantity(line.quantity)
                    .unitPrice(line.variant.effectiveSellingPrice())
                    .unitCost(cost)
                    .discountAmount(BigDecimal.ZERO)
                    .build();
            order.getItems().add(item);
        }
        order = orderRepo.save(order);

        // 4. Reserve stock for each line (status active; not yet dispatched)
        Map<Long, Warehouse> warehouseByVariant = new HashMap<>();
        for (ResolvedLine line : resolved) {
            warehouseByVariant.put(line.variant.getId(), line.warehouse);
        }
        for (OrderItem item : order.getItems()) {
            Warehouse wh = warehouseByVariant.getOrDefault(item.getVariant().getId(), warehouse);
            reservationSave(order, item, wh, item.getQuantity());
        }

        // 5. Payment record (COD-style flows settle on delivery)
        order.getPayments().add(Payment.builder()
                .order(order)
                .paymentMethod(paymentMethod == null || paymentMethod.isBlank() ? "COD" : paymentMethod)
                .amount(order.getGrandTotal())
                .status(PaymentStatus.pending)
                .build());

        // 6. Status history
        addHistory(order, null, OrderStatus.pending);

        return orderRepo.save(order);
    }

    // ------------------------------------------------------------- lifecycle

    /** Orders belonging to a customer, newest first. */
    @Transactional(readOnly = true)
    public List<Order> ordersOf(Customer customer) {
        return orderRepo.findByCustomerIdOrderByOrderDateDesc(customer.getId());
    }

    @Transactional(readOnly = true)
    public Order getOrderForCustomer(Long orderId, Customer customer) {
        Order order = orderRepo.findById(orderId)
                .orElseThrow(() -> new InvalidOperationException("Order not found: " + orderId));
        if (!order.getCustomer().getId().equals(customer.getId())) {
            throw new InvalidOperationException("Order not found: " + orderId);
        }
        return order;
    }

    @Transactional
    public Order confirm(Long orderId) {
        return transition(orderId, OrderStatus.pending, OrderStatus.confirmed);
    }

    @Transactional
    public Order startProcessing(Long orderId) {
        return transition(orderId, OrderStatus.confirmed, OrderStatus.processing);
    }

    /** Physically dispatch: post stock-out movements and complete reservations. */
    @Transactional
    public Order dispatch(Long orderId, String courier) {
        Order order = load(orderId);
        if (order.getStatus() != OrderStatus.processing && order.getStatus() != OrderStatus.confirmed) {
            throw new InvalidOperationException("Only confirmed/processing orders can be dispatched.");
        }
        inventoryService.dispatchOrderItems(order);

        order.getDeliveryShipments().add(DeliveryShipment.builder()
                .order(order)
                .courier(courier == null || courier.isBlank() ? "Manual" : courier)
                .status("in_transit")
                .shippedAt(LocalDateTime.now())
                .build());

        return changeStatus(order, OrderStatus.shipped);
    }

    @Transactional
    public Order deliver(Long orderId) {
        Order order = load(orderId);
        if (order.getStatus() != OrderStatus.shipped) {
            throw new InvalidOperationException("Only shipped orders can be marked delivered.");
        }
        // COD settles when the customer receives the goods
        order.getPayments().forEach(p -> {
            if (p.getStatus() == PaymentStatus.pending) {
                p.setStatus(PaymentStatus.paid);
            }
        });
        order.getDeliveryShipments().forEach(d -> {
            if (d.getStatus() != null && !d.getStatus().equals("delivered")) {
                d.setStatus("delivered");
                d.setDeliveredAt(LocalDateTime.now());
            }
        });
        return changeStatus(order, OrderStatus.delivered);
    }

    /** Cancel and release all held stock. */
    @Transactional
    public Order cancel(Long orderId) {
        Order order = load(orderId);
        if (order.getStatus() == OrderStatus.shipped || order.getStatus() == OrderStatus.delivered) {
            throw new InvalidOperationException("Shipped or delivered orders cannot be cancelled.");
        }
        if (order.getStatus() == OrderStatus.cancelled) {
            return order;
        }
        inventoryService.releaseReservations(order);
        order.getPayments().forEach(p -> {
            if (p.getStatus() == PaymentStatus.pending) {
                p.setStatus(PaymentStatus.refunded);
            }
        });
        return changeStatus(order, OrderStatus.cancelled);
    }

    // ------------------------------------------------------------- helpers

    private Order transition(Long orderId, OrderStatus from, OrderStatus to) {
        Order order = load(orderId);
        if (order.getStatus() != from) {
            throw new InvalidOperationException(
                    "Expected status '" + from + "' but order is '" + order.getStatus() + "'.");
        }
        return changeStatus(order, to);
    }

    private Order changeStatus(Order order, OrderStatus to) {
        OrderStatus from = order.getStatus();
        order.setStatus(to);
        addHistory(order, from, to);
        return orderRepo.save(order);
    }

    private void addHistory(Order order, OrderStatus from, OrderStatus to) {
        order.getStatusHistory().add(OrderStatusHistory.builder()
                .order(order)
                .oldStatus(from != null ? from.name() : null)
                .newStatus(to.name())
                .build());
    }

    private Order load(Long orderId) {
        return orderRepo.findById(orderId)
                .orElseThrow(() -> new InvalidOperationException("Order not found: " + orderId));
    }

    private InventoryBalance lockBalanceOrThrow(ProductVariant variant, Warehouse warehouse) {
        return inventoryService.lockBalance(variant.getId(), warehouse.getId())
                .orElseThrow(() -> new InsufficientStockException(
                        "'" + label(variant) + "' has no stock record and cannot be sold."));
    }

    private void reservationSave(Order order, OrderItem item, Warehouse warehouse, int qty) {
        StockReservation reservation = StockReservation.builder()
                .orderItem(item)
                .variant(item.getVariant())
                .warehouse(warehouse)
                .quantity(qty)
                .status(ReservationStatus.active)
                .expiresAt(LocalDateTime.now().plusHours(24))
                .build();
        reservationRepo.save(reservation);
    }

    private CustomerAddress saveAddress(Customer customer, String address, String city, String postalCode) {
        if (address == null || address.isBlank() || city == null || city.isBlank()) {
            throw new InvalidOperationException("A delivery address and city are required.");
        }
        CustomerAddress shipping = CustomerAddress.builder()
                .customer(customer)
                .addressType("delivery")
                .address(address.trim())
                .city(city.trim())
                .postalCode(postalCode)
                .isDefault(true)
                .build();
        return addressRepo.save(shipping);
    }

    private static String label(ProductVariant variant) {
        Product product = variant.getProduct();
        String base = product != null ? product.getName() : variant.getSku();
        if (variant.getSize() != null) {
            base += " (" + variant.getSize().getSizeName() + ")";
        }
        if (variant.getColor() != null) {
            base += " / " + variant.getColor().getColorName();
        }
        return base;
    }

    private record ResolvedLine(ProductVariant variant, int quantity, Warehouse warehouse) {}
}
