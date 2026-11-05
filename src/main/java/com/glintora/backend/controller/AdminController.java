package com.glintora.backend.controller;

import com.glintora.backend.dto.DashboardStats;
import com.glintora.backend.dto.LowStockLine;
import com.glintora.backend.dto.StockRow;
import com.glintora.backend.model.*;
import com.glintora.backend.model.enums.MovementType;
import com.glintora.backend.model.enums.OrderStatus;
import com.glintora.backend.model.enums.ProductStatus;
import com.glintora.backend.model.enums.ReservationStatus;
import com.glintora.backend.repository.*;
import com.glintora.backend.service.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.*;

/**
 * Internal operations dashboard (guide §3–§12): catalogue, inventory,
 * purchasing, shipments, receiving, orders, customers and review moderation.
 */
@Controller
@RequestMapping("/admin")
@RequiredArgsConstructor
public class AdminController {

    private final DashboardService dashboardService;
    private final CatalogAdminService catalogAdminService;
    private final InventoryService inventoryService;
    private final PurchasingService purchasingService;
    private final OrderService orderService;
    private final ProductRepository productRepo;
    private final BrandRepository brandRepo;
    private final CategoryRepository categoryRepo;
    private final SizeRepository sizeRepo;
    private final ColorRepository colorRepo;
    private final WarehouseRepository warehouseRepo;
    private final OrderRepository orderRepo;
    private final InventoryMovementRepository movementRepo;
    private final SupplierRepository supplierRepo;
    private final ShipmentRepository shipmentRepo;
    private final PurchaseOrderRepository poRepo;
    private final GoodsReceiptRepository receiptRepo;
    private final CustomerRepository customerRepo;
    private final ProductReviewRepository reviewRepo;
    private final InventoryBalanceRepository balanceRepo;

    /**
     * Sidebar badge counts (guide §3): pending reviews, reorder suggestions
     * and unconfirmed orders. Available on every admin page via fragments.
     */
    @org.springframework.web.bind.annotation.ModelAttribute
    public void sidebarBadges(Model model) {
        model.addAttribute("pendingReviewCount", reviewRepo.countByStatus("pending"));
        model.addAttribute("reorderCount", dashboardService.lowStockLines().size());
        model.addAttribute("pendingOrderCount", orderRepo.countByStatus(OrderStatus.pending));
    }

    // ---------------------------------------------------------- dashboard

    /** Guide §4: KPI cards, sales chart, inventory health, attention, activity. */
    @GetMapping
    public String dashboard(@RequestParam(defaultValue = "30") int days, Model model) {
        int period = days == 7 || days == 90 ? days : 30;
        DashboardStats stats = dashboardService.stats(period);
        model.addAttribute("stats", stats);
        model.addAttribute("days", period);
        model.addAttribute("recentOrders", dashboardService.recentOrders());
        model.addAttribute("recentMovements", movementRepo.findTop50ByOrderByCreatedAtDesc());
        return "admin/dashboard";
        }

    // ----------------------------------------------------------- products

    /** Guide §5: summary indicators, search, filters, stock-aware listing. */
    @GetMapping("/products")
    public String products(@RequestParam(required = false) String q,
                           @RequestParam(required = false) Long category,
                           @RequestParam(required = false) Long brand,
                           @RequestParam(required = false) String status,
                           Model model) {
        ProductStatus statusFilter = null;
        if (status != null && !status.isBlank()) {
            try {
                statusFilter = ProductStatus.valueOf(status);
            } catch (IllegalArgumentException ignored) {
                // unknown status chip — show all
            }
        }
        String needle = q == null || q.isBlank() ? null : q.trim();
        List<Product> products = productRepo.searchAll(needle, category, brand, statusFilter);

        // Available stock per product (sum across its variants)
        Map<Long, Integer> available = new HashMap<>();
        Set<Long> variantIds = new HashSet<>();
        products.forEach(p -> p.getVariants().forEach(v -> variantIds.add(v.getId())));
        Map<Long, Integer> availableByVariant = inventoryService.availableByVariant(variantIds);
        for (Product p : products) {
            int total = p.getVariants().stream()
                    .mapToInt(v -> availableByVariant.getOrDefault(v.getId(), 0))
                    .sum();
            available.put(p.getId(), total);
        }

        long outOfStock = products.stream().filter(p -> available.getOrDefault(p.getId(), 0) <= 0).count();
        model.addAttribute("products", products);
        model.addAttribute("availableByProduct", available);
        model.addAttribute("q", q);
        model.addAttribute("categoryFilter", category);
        model.addAttribute("brandFilter", brand);
        model.addAttribute("statusFilter", status);
        model.addAttribute("categories", categoryRepo.findAllByOrderByNameAsc());
        model.addAttribute("brands", brandRepo.findAllByOrderByNameAsc());
        model.addAttribute("totalProducts", productRepo.count());
        model.addAttribute("publishedCount", productRepo.countByStatus(ProductStatus.published));
        model.addAttribute("draftCount", productRepo.countByStatus(ProductStatus.draft));
        model.addAttribute("pendingCount", productRepo.countByStatus(ProductStatus.pending_review));
        model.addAttribute("outOfStockCount", outOfStock);
        return "admin/products";
    }

    @GetMapping("/products/new")
    public String newProductForm(Model model) {
        fillProductForm(model, null);
        return "admin/product-form";
    }

    @PostMapping("/products/new")
    public String createProduct(@RequestParam String productCode,
                                @RequestParam String name,
                                @RequestParam(required = false) Long brandId,
                                @RequestParam(required = false) Long categoryId,
                                @RequestParam(required = false) String description,
                                @RequestParam(required = false) String material,
                                @RequestParam(required = false) BigDecimal defaultSellingPrice,
                                @RequestParam(required = false) BigDecimal costPrice,
                                RedirectAttributes ra) {
        try {
            Product product = catalogAdminService.createProduct(productCode, name, brandId, categoryId,
                    description, material, defaultSellingPrice, costPrice);
            ra.addFlashAttribute("message", "Product " + product.getProductCode() + " created as draft.");
            return "redirect:/admin/products/" + product.getId() + "/edit";
        } catch (InvalidOperationException e) {
            ra.addFlashAttribute("error", e.getMessage());
            return "redirect:/admin/products/new";
        }
    }

    @GetMapping("/products/{id}/edit")
    public String editProductForm(@PathVariable Long id, Model model) {
        fillProductForm(model, catalogAdminService.getProduct(id));
        return "admin/product-edit";
    }

    @PostMapping("/products/{id}/edit")
    public String updateProduct(@PathVariable Long id,
                                @RequestParam String name,
                                @RequestParam(required = false) Long brandId,
                                @RequestParam(required = false) Long categoryId,
                                @RequestParam(required = false) String description,
                                @RequestParam(required = false) String material,
                                @RequestParam(required = false) BigDecimal defaultSellingPrice,
                                @RequestParam(required = false) BigDecimal costPrice,
                                RedirectAttributes ra) {
        try {
            catalogAdminService.updateProduct(id, name, brandId, categoryId, description,
                    material, defaultSellingPrice, costPrice);
            ra.addFlashAttribute("message", "Product updated.");
        } catch (InvalidOperationException e) {
            ra.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/admin/products/" + id + "/edit";
    }

    @PostMapping("/products/{id}/status")
    public String changeStatus(@PathVariable Long id, @RequestParam String status,
                               RedirectAttributes ra) {
        try {
            catalogAdminService.changeStatus(id, ProductStatus.valueOf(status));
            ra.addFlashAttribute("message", "Status changed to " + status + ".");
        } catch (IllegalArgumentException | InvalidOperationException e) {
            ra.addFlashAttribute("error", "Cannot change status: " + e.getMessage());
        }
        return "redirect:/admin/products/" + id + "/edit";
    }

    @PostMapping("/products/{id}/variants")
    public String addVariant(@PathVariable Long id,
                             @RequestParam(required = false) String sku,
                             @RequestParam(required = false) Long sizeId,
                             @RequestParam(required = false) Long colorId,
                             @RequestParam(required = false) BigDecimal sellingPrice,
                             @RequestParam(required = false) BigDecimal weight,
                             RedirectAttributes ra) {
        try {
            ProductVariant variant = catalogAdminService.addVariant(id, sku, sizeId, colorId, sellingPrice, weight);
            ra.addFlashAttribute("message", "Variant " + variant.getSku() + " added.");
        } catch (InvalidOperationException e) {
            ra.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/admin/products/" + id + "/edit";
    }

    @PostMapping("/variants/{variantId}/delete")
    public String deleteVariant(@PathVariable Long variantId,
                                @RequestParam Long productId, RedirectAttributes ra) {
        try {
            catalogAdminService.deleteVariant(variantId);
            ra.addFlashAttribute("message", "Variant deleted.");
        } catch (InvalidOperationException e) {
            ra.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/admin/products/" + productId + "/edit";
    }

    @PostMapping("/products/{id}/images")
    public String addImage(@PathVariable Long id,
                           @RequestParam String imageUrl,
                           @RequestParam(required = false) boolean primary,
                           RedirectAttributes ra) {
        try {
            catalogAdminService.addImage(id, imageUrl, primary);
            ra.addFlashAttribute("message", "Image added.");
        } catch (InvalidOperationException e) {
            ra.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/admin/products/" + id + "/edit";
    }

    // ------------------------------------------------- categories & brands

    @GetMapping("/categories")
    public String categories(Model model) {
        List<Category> categories = categoryRepo.findAllByOrderByNameAsc();
        Map<Long, Long> productCounts = new HashMap<>();
        productRepo.findAll().forEach(p -> {
            if (p.getCategory() != null) {
                productCounts.merge(p.getCategory().getId(), 1L, Long::sum);
            }
        });
        model.addAttribute("categories", categories);
        model.addAttribute("productCounts", productCounts);
        return "admin/categories";
    }

    @PostMapping("/categories")
    public String createCategory(@RequestParam String name,
                                 @RequestParam(required = false) Long parentId,
                                 RedirectAttributes ra) {
        try {
            Category parent = parentId == null ? null : categoryRepo.findById(parentId).orElse(null);
            categoryRepo.save(Category.builder().name(name.trim()).parentCategory(parent).build());
            ra.addFlashAttribute("message", "Category created.");
        } catch (Exception e) {
            ra.addFlashAttribute("error", "Cannot create category: " + e.getMessage());
        }
        return "redirect:/admin/categories";
    }

    @GetMapping("/brands")
    public String brands(Model model) {
        List<Brand> brands = brandRepo.findAllByOrderByNameAsc();
        Map<Long, Long> productCounts = new HashMap<>();
        productRepo.findAll().forEach(p -> {
            if (p.getBrand() != null) {
                productCounts.merge(p.getBrand().getId(), 1L, Long::sum);
            }
        });
        model.addAttribute("brands", brands);
        model.addAttribute("productCounts", productCounts);
        return "admin/brands";
    }

    @PostMapping("/brands")
    public String createBrand(@RequestParam String name,
                              @RequestParam(required = false) String logoUrl,
                              RedirectAttributes ra) {
        try {
            brandRepo.save(Brand.builder().name(name.trim()).logoUrl(logoUrl).build());
            ra.addFlashAttribute("message", "Brand created.");
        } catch (Exception e) {
            ra.addFlashAttribute("error", "Cannot create brand: " + e.getMessage());
        }
        return "redirect:/admin/brands";
    }

    // ------------------------------------------------------------ reviews

    @GetMapping("/reviews")
    public String reviews(@RequestParam(required = false) String status, Model model) {
        String filter = status == null || status.isBlank() || "all".equals(status) ? null : status;
        List<ProductReview> reviews = filter == null
                ? reviewRepo.findAllByOrderByCreatedAtDesc()
                : reviewRepo.findByStatusOrderByCreatedAtDesc(filter);
        model.addAttribute("reviews", reviews);
        model.addAttribute("statusFilter", filter == null ? "all" : filter);
        model.addAttribute("pendingCount", reviewRepo.countByStatus("pending"));
        model.addAttribute("approvedCount", reviewRepo.countByStatus("approved"));
        model.addAttribute("rejectedCount", reviewRepo.countByStatus("rejected"));
        return "admin/reviews";
    }

    @PostMapping("/reviews/{id}/status")
    public String changeReviewStatus(@PathVariable Long id, @RequestParam String status,
                                     RedirectAttributes ra) {
        if (!Set.of("pending", "approved", "rejected").contains(status)) {
            ra.addFlashAttribute("error", "Unknown review status.");
            return "redirect:/admin/reviews";
        }
        return reviewRepo.findById(id).map(review -> {
            review.setStatus(status);
            reviewRepo.save(review);
            ra.addFlashAttribute("message", "Review " + status + ".");
            return "redirect:/admin/reviews";
        }).orElse("redirect:/admin/reviews");
    }

    // ---------------------------------------------------------- inventory

    /** Guide §6 stock overview: one row per variant per warehouse. */
    @GetMapping("/inventory")
    public String inventory(@RequestParam(required = false) Long warehouse,
                            @RequestParam(required = false) String status,
                            @RequestParam(required = false) String q,
                            Model model) {
        String statusFilter = status == null || status.isBlank() || "all".equals(status) ? null : status;
        List<StockRow> rows = inventoryService.stockOverview(warehouse, statusFilter, q);

        model.addAttribute("rows", rows);
        model.addAttribute("warehouseFilter", warehouse);
        model.addAttribute("statusFilter", statusFilter == null ? "all" : statusFilter);
        model.addAttribute("q", q);
        model.addAttribute("warehouses", warehouseRepo.findAll());
        model.addAttribute("variants", allVariantsForSelect());
        model.addAttribute("movementTypes", MovementType.values());
        model.addAttribute("inCount", rows.stream().filter(r -> "in".equals(r.getStatus())).count());
        model.addAttribute("lowCount", rows.stream().filter(r -> "low".equals(r.getStatus())).count());
        model.addAttribute("outCount", rows.stream().filter(r -> "out".equals(r.getStatus())).count());
        return "admin/inventory";
    }

    /** Receive physical stock into a warehouse; only accepted units add to stock. */
    @PostMapping("/inventory/receive")
    public String receiveStock(@RequestParam Long variantId,
                               @RequestParam int quantityReceived,
                               @RequestParam(defaultValue = "0") int quantityRejected,
                               @RequestParam Long warehouseId,
                               @RequestParam(required = false) String receivedBy,
                               RedirectAttributes ra) {
        try {
            GoodsReceipt receipt = inventoryService.receiveGoods(warehouseId,
                    receivedBy == null || receivedBy.isBlank() ? "admin" : receivedBy,
                    Map.of(variantId, new InventoryService.ReceiveLine(quantityReceived, quantityRejected)),
                    null);
            ra.addFlashAttribute("message", "Goods receipt #" + receipt.getId() + " recorded.");
        } catch (InvalidOperationException | InsufficientStockException e) {
            ra.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/admin/inventory";
    }

    /** Manual adjustment; negative deltas write stock off, positive add it. */
    @PostMapping("/inventory/adjust")
    public String adjustStock(@RequestParam Long variantId,
                              @RequestParam int delta,
                              @RequestParam Long warehouseId,
                              @RequestParam(required = false) String movementType,
                              @RequestParam(required = false) String reason,
                              RedirectAttributes ra) {
        try {
            MovementType type = null;
            if (movementType != null && !movementType.isBlank()) {
                type = MovementType.valueOf(movementType);
            }
            inventoryService.adjust(variantId, warehouseId, delta, type, reason, "admin");
            ra.addFlashAttribute("message", "Adjustment posted.");
        } catch (InvalidOperationException | InsufficientStockException | IllegalArgumentException e) {
            ra.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/admin/inventory";
    }

    // ------------------------------------------------------ stock movements

    /** Guide §6: append-only movement log with actor and reason. */
    @GetMapping("/movements")
    public String movements(@RequestParam(required = false) String type,
                            @RequestParam(required = false) Long warehouse,
                            Model model) {
        MovementType typeFilter = null;
        if (type != null && !type.isBlank() && !"all".equals(type)) {
            try {
                typeFilter = MovementType.valueOf(type);
            } catch (IllegalArgumentException ignored) {
                // fall through to unfiltered
            }
        }
        model.addAttribute("movements", movementRepo.search(typeFilter, warehouse));
        model.addAttribute("typeFilter", type == null || type.isBlank() ? "all" : type);
        model.addAttribute("warehouseFilter", warehouse);
        model.addAttribute("movementTypes", MovementType.values());
        model.addAttribute("warehouses", warehouseRepo.findAll());
        return "admin/movements";
    }

    // ------------------------------------------------------ purchase orders

    @GetMapping("/purchase-orders")
    public String purchaseOrders(@RequestParam(required = false) String status, Model model) {
        String filter = status == null || status.isBlank() || "all".equals(status) ? null : status;
        List<PurchaseOrder> all = purchasingService.allPurchaseOrders();
        List<PurchaseOrder> orders = filter == null ? all
                : all.stream().filter(po -> filter.equals(po.getStatus())).toList();

        Map<String, Long> counts = new LinkedHashMap<>();
        counts.put("all", (long) all.size());
        for (String s : List.of("draft", "ordered", "partial", "received", "cancelled")) {
            counts.put(s, all.stream().filter(po -> s.equals(po.getStatus())).count());
        }
        model.addAttribute("purchaseOrders", orders);
        model.addAttribute("statusCounts", counts);
        model.addAttribute("statusFilter", filter == null ? "all" : filter);
        return "admin/purchase-orders";
    }

    @GetMapping("/purchase-orders/new")
    public String newPurchaseOrder(Model model) {
        model.addAttribute("suppliers", supplierRepo.findAllByOrderByNameAsc());
        model.addAttribute("variants", allVariantsForSelect());
        return "admin/purchase-order-form";
    }

    @PostMapping("/purchase-orders")
    public String createPurchaseOrder(@RequestParam Long supplierId,
                                      @RequestParam(required = false) String expectedDate,
                                      @RequestParam(required = false) List<Long> variantId,
                                      @RequestParam(required = false) List<Integer> quantity,
                                      @RequestParam(required = false) List<BigDecimal> unitCost,
                                      RedirectAttributes ra) {
        try {
            LocalDate expected = expectedDate == null || expectedDate.isBlank()
                    ? null : LocalDate.parse(expectedDate);
            PurchaseOrder po = purchasingService.createDraftPo(supplierId, expected,
                    variantId, quantity, unitCost);
            ra.addFlashAttribute("message", "Draft purchase order #" + po.getId() + " created.");
            return "redirect:/admin/purchase-orders/" + po.getId();
        } catch (InvalidOperationException | java.time.format.DateTimeParseException e) {
            ra.addFlashAttribute("error", e.getMessage());
            return "redirect:/admin/purchase-orders/new";
        }
    }

    @GetMapping("/purchase-orders/{id}")
    public String purchaseOrderDetail(@PathVariable Long id, Model model) {
        PurchaseOrder po = purchasingService.getPo(id);
        model.addAttribute("po", po);
        model.addAttribute("receivingLines", purchasingService.receivingLines(id));
        model.addAttribute("shipments", shipmentRepo.findByPurchaseOrderId(id));
        model.addAttribute("receipts", receiptRepo.findAllByOrderByReceivedAtDesc().stream()
                .filter(r -> r.getShipment() != null
                        && r.getShipment().getPurchaseOrder() != null
                        && r.getShipment().getPurchaseOrder().getId().equals(id))
                .toList());
        model.addAttribute("receivableShipments", shipmentRepo.findByPurchaseOrderId(id).stream()
                .filter(s -> Set.of("arrived", "receiving").contains(s.getStatus()))
                .toList());
        model.addAttribute("warehouses", warehouseRepo.findAll());
        model.addAttribute("nextStatuses", nextPoStatuses(po.getStatus()));
        return "admin/purchase-order-detail";
    }

    @PostMapping("/purchase-orders/{id}/status")
    public String changePoStatus(@PathVariable Long id, @RequestParam String status,
                                 RedirectAttributes ra) {
        try {
            purchasingService.changePoStatus(id, status);
            ra.addFlashAttribute("message", "Purchase order is now " + status + ".");
        } catch (InvalidOperationException e) {
            ra.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/admin/purchase-orders/" + id;
    }

    @PostMapping("/purchase-orders/{id}/shipments")
    public String createShipment(@PathVariable Long id,
                                 @RequestParam(required = false) String reference,
                                 @RequestParam(required = false) String dispatchDate,
                                 @RequestParam String expectedArrival,
                                 RedirectAttributes ra) {
        try {
            Shipment shipment = purchasingService.createShipment(id,
                    reference == null || reference.isBlank() ? null : reference,
                    dispatchDate == null || dispatchDate.isBlank() ? null : LocalDate.parse(dispatchDate),
                    LocalDate.parse(expectedArrival));
            ra.addFlashAttribute("message", "Shipment booked against PO #" + id
                    + " (expected " + shipment.getExpectedArrivalDate() + ").");
        } catch (InvalidOperationException | java.time.format.DateTimeParseException e) {
            ra.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/admin/purchase-orders/" + id;
    }

    // ----------------------------------------------------------- suppliers

    @GetMapping("/suppliers")
    public String suppliers(Model model) {
        List<Supplier> suppliers = supplierRepo.findAllByOrderByNameAsc();
        List<PurchaseOrder> allPos = poRepo.findAllByOrderByOrderDateDesc();
        Map<Long, Long> openOrders = new HashMap<>();
        Map<Long, Long> totalOrders = new HashMap<>();
        Map<Long, BigDecimal> spend = new HashMap<>();
        for (PurchaseOrder po : allPos) {
            Long sid = po.getSupplier().getId();
            totalOrders.merge(sid, 1L, Long::sum);
            if (Set.of("draft", "ordered", "partial").contains(po.getStatus())) {
                openOrders.merge(sid, 1L, Long::sum);
            }
            if ("received".equals(po.getStatus())) {
                spend.merge(sid, po.totalValue(), BigDecimal::add);
            }
        }
        model.addAttribute("suppliers", suppliers);
        model.addAttribute("openOrders", openOrders);
        model.addAttribute("totalOrders", totalOrders);
        model.addAttribute("spend", spend);
        return "admin/suppliers";
    }

    @PostMapping("/suppliers")
    public String createSupplier(@RequestParam String name,
                                 @RequestParam(required = false) String country,
                                 @RequestParam(required = false) String contactDetails,
                                 RedirectAttributes ra) {
        try {
            supplierRepo.save(Supplier.builder()
                    .name(name.trim())
                    .country(country)
                    .contactDetails(contactDetails)
                    .build());
            ra.addFlashAttribute("message", "Supplier added.");
        } catch (Exception e) {
            ra.addFlashAttribute("error", "Cannot add supplier: " + e.getMessage());
        }
        return "redirect:/admin/suppliers";
    }

    // ----------------------------------------------------------- shipments

    /** Guide §7 incoming shipments with the Ordered→…→Completed timeline. */
    @GetMapping("/shipments")
    public String shipments(@RequestParam(required = false) String status,
                            @RequestParam(required = false) boolean overdueOnly,
                            Model model) {
        String filter = status == null || status.isBlank() || "all".equals(status) ? null : status;
        LocalDate today = LocalDate.now();
        Set<Long> overdueIds = new HashSet<>();
        shipmentRepo.findOverdue(today).forEach(s -> overdueIds.add(s.getId()));

        List<Shipment> shipments = purchasingService.allShipments().stream()
                .filter(s -> filter == null || filter.equals(s.getStatus()))
                .filter(s -> !overdueOnly || overdueIds.contains(s.getId()))
                .toList();

        Map<String, Long> counts = new LinkedHashMap<>();
        List<Shipment> all = purchasingService.allShipments();
        counts.put("all", (long) all.size());
        for (String s : List.of("ordered", "dispatched", "in_transit", "arrived", "receiving", "completed", "cancelled")) {
            counts.put(s, all.stream().filter(x -> s.equals(x.getStatus())).count());
        }
        model.addAttribute("shipments", shipments);
        model.addAttribute("statusCounts", counts);
        model.addAttribute("statusFilter", filter == null ? "all" : filter);
        model.addAttribute("overdueOnly", overdueOnly);
        model.addAttribute("overdueIds", overdueIds);
        model.addAttribute("openPurchaseOrders", allPosOpen());
        Map<Long, List<String>> nextActions = new LinkedHashMap<>();
        for (Shipment s : shipments) {
            nextActions.put(s.getId(), nextShipmentStatuses(s.getStatus()));
        }
        model.addAttribute("nextActions", nextActions);
        return "admin/shipments";
    }

    /** Legal successor states for the shipment timeline buttons. */
    private List<String> nextShipmentStatuses(String current) {
        return switch (current == null ? "" : current) {
            case "ordered" -> List.of("dispatched", "cancelled");
            case "dispatched" -> List.of("in_transit", "cancelled");
            case "in_transit" -> List.of("arrived", "cancelled");
            case "arrived" -> List.of("receiving", "cancelled");
            case "receiving" -> List.of("completed");
            default -> List.of();
        };
    }

    @PostMapping("/shipments")
    public String createShipmentFromList(@RequestParam Long purchaseOrderId,
                                         @RequestParam(required = false) String reference,
                                         @RequestParam String expectedArrival,
                                         RedirectAttributes ra) {
        try {
            purchasingService.createShipment(purchaseOrderId,
                    reference == null || reference.isBlank() ? null : reference,
                    LocalDate.now(), LocalDate.parse(expectedArrival));
            ra.addFlashAttribute("message", "Shipment booked.");
        } catch (InvalidOperationException | java.time.format.DateTimeParseException e) {
            ra.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/admin/shipments";
    }

    @PostMapping("/shipments/{id}/status")
    public String changeShipmentStatus(@PathVariable Long id, @RequestParam String status,
                                       RedirectAttributes ra) {
        try {
            purchasingService.changeShipmentStatus(id, status);
            ra.addFlashAttribute("message", "Shipment is now " + status + ".");
        } catch (InvalidOperationException e) {
            ra.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/admin/shipments";
    }

    // ------------------------------------------------------------ receiving

    /** Guide §7 goods receiving: history + partial receipt against a shipment. */
    @GetMapping("/receiving")
    public String receiving(@RequestParam(required = false) Long shipment, Model model) {
        List<Shipment> openShipments = shipmentRepo.findAllByOrderByExpectedArrivalDateAsc().stream()
                .filter(s -> !Set.of("completed", "cancelled").contains(s.getStatus()))
                .toList();
        model.addAttribute("openShipments", openShipments);
        model.addAttribute("receipts", receiptRepo.findAllByOrderByReceivedAtDesc());
        model.addAttribute("warehouses", warehouseRepo.findAll());
        model.addAttribute("selectedShipment", shipment);

        if (shipment != null) {
            Shipment selected = purchasingService.getShipment(shipment);
            model.addAttribute("selected", selected);
            model.addAttribute("receivingLines",
                    purchasingService.receivingLines(selected.getPurchaseOrder().getId()));
        }
        return "admin/receiving";
    }

    /** Post a partial receipt against a shipment; accepted units add to stock. */
    @PostMapping("/receiving")
    public String postReceipt(@RequestParam Long shipmentId,
                              @RequestParam Long warehouseId,
                              @RequestParam(required = false) String receivedBy,
                              @RequestParam List<Long> variantId,
                              @RequestParam List<Integer> received,
                              @RequestParam(required = false) List<Integer> rejected,
                              RedirectAttributes ra) {
        try {
            Map<Long, InventoryService.ReceiveLine> lines = new LinkedHashMap<>();
            for (int i = 0; i < variantId.size(); i++) {
                Long vid = variantId.get(i);
                if (vid == null) continue;
                int recv = received.get(i) == null ? 0 : received.get(i);
                if (recv <= 0) continue;
                int rej = rejected != null && i < rejected.size() && rejected.get(i) != null
                        ? rejected.get(i) : 0;
                lines.put(vid, new InventoryService.ReceiveLine(recv, rej));
            }
            if (lines.isEmpty()) {
                throw new InvalidOperationException("Enter a received quantity for at least one line.");
            }
            Shipment shipment = purchasingService.getShipment(shipmentId);
            String by = receivedBy == null || receivedBy.isBlank() ? "admin" : receivedBy;
            GoodsReceipt receipt = inventoryService.receiveGoods(warehouseId, by, lines, shipmentId);

            // Keep PO and shipment states in step with what was physically received.
            Long poId = shipment.getPurchaseOrder().getId();
            boolean complete = purchasingService.receivingLines(poId).stream()
                    .allMatch(l -> l.remaining() <= 0);
            purchasingService.changeShipmentStatus(shipmentId, complete ? "completed" : "receiving");
            try {
                purchasingService.changePoStatus(poId, complete ? "received" : "partial");
            } catch (InvalidOperationException notLegal) {
                // PO already in a later state — leave it alone.
            }
            ra.addFlashAttribute("message", "Goods receipt #" + receipt.getId() + " posted.");
            return "redirect:/admin/receiving?shipment=" + shipmentId;
        } catch (InvalidOperationException | InsufficientStockException e) {
            ra.addFlashAttribute("error", e.getMessage());
            return "redirect:/admin/receiving?shipment=" + shipmentId;
        }
    }

    // ---------------------------------------------------------- warehouses

    @GetMapping("/warehouses")
    public String warehouses(Model model) {
        List<Warehouse> warehouses = warehouseRepo.findAll();
        Map<Long, Long> variantCounts = new HashMap<>();
        Map<Long, Long> units = new HashMap<>();
        for (Object[] row : balanceRepo.warehouseStockSummary()) {
            variantCounts.put((Long) row[0], ((Number) row[1]).longValue());
            units.put((Long) row[0], ((Number) row[2]).longValue());
        }
        Map<Long, BigDecimal> values = new HashMap<>();
        for (Object[] row : balanceRepo.valueByWarehouse()) {
            values.put((Long) row[0], (BigDecimal) row[1]);
        }
        Map<Long, Long> lowStock = new HashMap<>();
        for (StockRow row : inventoryService.stockOverview(null, null, null)) {
            if (!"in".equals(row.getStatus())) {
                lowStock.merge(row.getWarehouseId(), 1L, Long::sum);
            }
        }
        model.addAttribute("warehouses", warehouses);
        model.addAttribute("variantCounts", variantCounts);
        model.addAttribute("units", units);
        model.addAttribute("values", values);
        model.addAttribute("lowStock", lowStock);
        return "admin/warehouses";
    }

    // ------------------------------------------------------ reorder planning

    /** Guide §6 reorder planning: suggestions create draft POs, never live orders. */
    @GetMapping("/reorder")
    public String reorder(Model model) {
        model.addAttribute("lines", dashboardService.lowStockLines());
        model.addAttribute("suppliers", supplierRepo.findAllByOrderByNameAsc());
        return "admin/reorder";
    }

    @PostMapping("/reorder/draft-po")
    public String draftPoFromSuggestion(@RequestParam Long ruleId,
                                        @RequestParam Long supplierId,
                                        @RequestParam(required = false) Integer quantity,
                                        RedirectAttributes ra) {
        try {
            LowStockLine line = dashboardService.lowStockLines().stream()
                    .filter(l -> l.getRuleId().equals(ruleId))
                    .findFirst()
                    .orElseThrow(() -> new InvalidOperationException("Reorder suggestion no longer applies."));
            PurchaseOrder po = purchasingService.createDraftFromSuggestion(line, supplierId, quantity);
            ra.addFlashAttribute("message", "Draft purchase order #" + po.getId() + " created from suggestion.");
            return "redirect:/admin/purchase-orders/" + po.getId();
        } catch (InvalidOperationException e) {
            ra.addFlashAttribute("error", e.getMessage());
            return "redirect:/admin/reorder";
        }
    }

    // ------------------------------------------------------------- orders

    /** Guide §9: status-count tabs and payment + fulfillment columns. */
    @GetMapping("/orders")
    public String orders(@RequestParam(required = false) String status,
                         @RequestParam(required = false) String q,
                         Model model) {
        String filter = status == null || status.isBlank() || "all".equals(status) ? null : status;
        List<Order> orders = filter == null
                ? orderRepo.findAllByOrderByOrderDateDesc()
                : orderRepo.findByStatusOrderByOrderDateDesc(OrderStatus.valueOf(filter));

        String needle = q == null || q.isBlank() ? null : q.trim().toLowerCase();
        if (needle != null) {
            orders = orders.stream().filter(o ->
                    String.valueOf(o.getId()).contains(needle)
                            || o.getCustomer().getName().toLowerCase().contains(needle)
                            || o.getCustomer().getEmail().toLowerCase().contains(needle))
                    .toList();
        }

        Map<String, Long> counts = new LinkedHashMap<>();
        counts.put("all", orderRepo.count());
        for (OrderStatus s : OrderStatus.values()) {
            counts.put(s.name(), orderRepo.countByStatus(s));
        }
        model.addAttribute("orders", orders);
        model.addAttribute("statusCounts", counts);
        model.addAttribute("statusFilter", filter == null ? "all" : filter);
        model.addAttribute("q", q);
        return "admin/orders";
    }

    @GetMapping("/orders/{id}")
    public String orderDetail(@PathVariable Long id, Model model) {
        Order order = orderRepo.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Order not found"));
        model.addAttribute("order", order);
        model.addAttribute("statusHistory", order.getStatusHistory());
        return "admin/order-detail";
    }

    @PostMapping("/orders/{id}/confirm")
    public String confirmOrder(@PathVariable Long id, RedirectAttributes ra) {
        return runOrderAction(id, () -> orderService.confirm(id), "Order confirmed.", ra);
    }

    @PostMapping("/orders/{id}/process")
    public String processOrder(@PathVariable Long id, RedirectAttributes ra) {
        return runOrderAction(id, () -> orderService.startProcessing(id), "Order is now processing.", ra);
    }

    @PostMapping("/orders/{id}/dispatch")
    public String dispatchOrder(@PathVariable Long id,
                                @RequestParam(required = false) String courier,
                                RedirectAttributes ra) {
        return runOrderAction(id, () -> orderService.dispatch(id, courier),
                "Order dispatched and stock deducted.", ra);
    }

    @PostMapping("/orders/{id}/deliver")
    public String deliverOrder(@PathVariable Long id, RedirectAttributes ra) {
        return runOrderAction(id, () -> orderService.deliver(id), "Order delivered; payment settled.", ra);
    }

    @PostMapping("/orders/{id}/cancel")
    public String cancelOrder(@PathVariable Long id, RedirectAttributes ra) {
        return runOrderAction(id, () -> orderService.cancel(id), "Order cancelled; stock released.", ra);
    }

    // ----------------------------------------------------------- customers

    /** Guide §12 customer list: identity, order count, spend, last order. */
    @GetMapping("/customers")
    public String customers(@RequestParam(required = false) String q, Model model) {
        Map<Long, Object[]> stats = new HashMap<>();
        for (Object[] row : orderRepo.customerStats()) {
            stats.put((Long) row[0], row);
        }
        String needle = q == null || q.isBlank() ? null : q.trim().toLowerCase();
        List<Customer> customers = customerRepo.findAll().stream()
                .filter(c -> "ROLE_CUSTOMER".equals(c.getRole()))
                .filter(c -> needle == null
                        || c.getName().toLowerCase().contains(needle)
                        || c.getEmail().toLowerCase().contains(needle))
                .sorted(Comparator.comparing(Customer::getName))
                .toList();

        Map<Long, Long> orderCounts = new HashMap<>();
        Map<Long, BigDecimal> spend = new HashMap<>();
        Map<Long, String> lastOrders = new HashMap<>();
        stats.forEach((id, row) -> {
            orderCounts.put(id, ((Number) row[1]).longValue());
            spend.put(id, (BigDecimal) row[2]);
            if (row[3] != null) {
                lastOrders.put(id, String.valueOf(row[3]));
            }
        });
        model.addAttribute("customers", customers);
        model.addAttribute("orderCounts", orderCounts);
        model.addAttribute("spend", spend);
        model.addAttribute("lastOrders", lastOrders);
        model.addAttribute("q", q);
        return "admin/customers";
    }

    // ------------------------------------------------------------ helpers

    private List<PurchaseOrder> allPosOpen() {
        return poRepo.findAllByOrderByOrderDateDesc().stream()
                .filter(po -> Set.of("draft", "ordered", "partial").contains(po.getStatus()))
                .toList();
    }

    private List<String> nextPoStatuses(String current) {
        return switch (current == null ? "" : current) {
            case "draft" -> List.of("ordered", "cancelled");
            case "ordered" -> List.of("partial", "received", "cancelled");
            case "partial" -> List.of("received", "cancelled");
            default -> List.of();
        };
    }

    private List<ProductVariantRow> allVariantsForSelect() {
        List<ProductVariantRow> rows = new ArrayList<>();
        productRepo.findAll().forEach(p -> p.getVariants().forEach(v -> rows.add(new ProductVariantRow(
                v.getId(),
                v.getSku() + " — " + p.getName()
                        + (v.getSize() != null ? " / " + v.getSize().getSizeName() : "")
                        + (v.getColor() != null ? " / " + v.getColor().getColorName() : "")))));
        rows.sort(Comparator.comparing(ProductVariantRow::label));
        return rows;
    }

    public record ProductVariantRow(Long id, String label) {}

    private String runOrderAction(Long id, Runnable action, String success, RedirectAttributes ra) {
        try {
            action.run();
            ra.addFlashAttribute("message", success);
        } catch (InvalidOperationException | InsufficientStockException e) {
            ra.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/admin/orders/" + id;
    }

    private void fillProductForm(Model model, Product product) {
        model.addAttribute("product", product);
        model.addAttribute("brands", brandRepo.findAllByOrderByNameAsc());
        model.addAttribute("categories", categoryRepo.findAllByOrderByNameAsc());
        model.addAttribute("sizes", sizeRepo.findAllByOrderByIdAsc());
        model.addAttribute("colors", colorRepo.findAllByOrderByIdAsc());
        model.addAttribute("warehouses", warehouseRepo.findAll());
    }
}
