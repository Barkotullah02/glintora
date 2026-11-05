package com.glintora.backend.service;

import com.glintora.backend.dto.ProductCard;
import com.glintora.backend.dto.VariantView;
import com.glintora.backend.model.*;
import com.glintora.backend.model.enums.ProductStatus;
import com.glintora.backend.model.enums.ProductVariantStatus;
import com.glintora.backend.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.math.BigDecimal;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Read-side service for the storefront. Assembles product cards and detail
 * views, always deriving stock availability from the inventory service
 * rather than from product status.
 */
@Service
@RequiredArgsConstructor
public class CatalogService {

    private final ProductRepository productRepo;
    private final ProductReviewRepository reviewRepo;
    private final InventoryService inventoryService;

    /** Published products for the home page. */
    @Transactional(readOnly = true)
    public List<ProductCard> featured(int limit) {
        List<Product> products = productRepo.findTop8ByStatusOrderByNameAsc(ProductStatus.published);
        return toCards(products).stream().limit(limit).toList();
    }

    /** Newest published products — New Arrivals rail. */
    @Transactional(readOnly = true)
    public List<ProductCard> newArrivals(int limit) {
        return toCards(productRepo.findNewArrivals()).stream().limit(limit).toList();
    }

    /** Bestselling published products by units sold; falls back to featured. */
    @Transactional(readOnly = true)
    public List<ProductCard> bestsellers(int limit) {
        List<Object[]> rows = productRepo.bestsellersByUnitsSold();
        List<Product> products = new ArrayList<>();
        for (Object[] row : rows) {
            productRepo.findById((Long) row[0]).ifPresent(products::add);
            if (products.size() >= limit) {
                break;
            }
        }
        if (products.isEmpty()) {
            return featured(limit);
        }
        return toCards(products);
    }

    /** Filtered, published catalogue for the shop page. */
    @Transactional(readOnly = true)
    public List<ProductCard> search(String q, Long categoryId, Long brandId, Long sizeId) {
        String query = (q == null || q.isBlank()) ? null : q.trim();
        List<Product> products = productRepo.searchPublished(query, categoryId, brandId, sizeId);
        return toCards(products);
    }

    @Transactional(readOnly = true)
    public List<ProductCard> related(Long categoryId, Long excludeProductId) {
        if (categoryId == null) {
            return List.of();
        }
        return toCards(productRepo.findTop4ByCategoryIdAndIdNot(categoryId, excludeProductId));
    }

    /** Full detail view for the product page, or empty when not published. */
    @Transactional(readOnly = true)
    public Optional<ProductDetail> detail(String productCode) {
        return productRepo.findByProductCode(productCode)
                .filter(p -> p.getStatus() == ProductStatus.published)
                .map(this::buildDetail);
    }

    @Transactional(readOnly = true)
    public Optional<Product> byCode(String productCode) {
        return productRepo.findByProductCode(productCode);
    }

    private ProductDetail buildDetail(Product product) {
        List<ProductVariant> variants = product.getVariants().stream()
                .filter(v -> v.getStatus() == ProductVariantStatus.active)
                .toList();
        List<Long> variantIds = variants.stream().map(ProductVariant::getId).toList();

        Map<Long, Integer> onHand = inventoryService.onHandByVariant(variantIds);
        Map<Long, Integer> reserved = inventoryService.reservedByVariant(variantIds);

        List<VariantView> variantViews = variants.stream()
                .map(v -> {
                    int oh = onHand.getOrDefault(v.getId(), 0);
                    int rs = reserved.getOrDefault(v.getId(), 0);
                    int avail = oh - rs;
                    return VariantView.builder()
                            .id(v.getId())
                            .sku(v.getSku())
                            .sizeId(v.getSize() != null ? v.getSize().getId() : null)
                            .sizeName(v.getSize() != null ? v.getSize().getSizeName() : null)
                            .colorId(v.getColor() != null ? v.getColor().getId() : null)
                            .colorName(v.getColor() != null ? v.getColor().getColorName() : null)
                            .colorHex(v.getColor() != null ? v.getColor().getHexCode() : null)
                            .price(v.effectiveSellingPrice())
                            .onHand(oh)
                            .reserved(rs)
                            .available(avail)
                            .availableNow(avail > 0)
                            .build();
                })
                .toList();

        // Distinct colours of this product, for the swatch row
        List<VariantColor> colors = new ArrayList<>();
        for (VariantView v : variantViews) {
            if (v.getColorId() != null && colors.stream().noneMatch(c -> c.id().equals(v.getColorId()))) {
                colors.add(new VariantColor(v.getColorId(), v.getColorName(),
                        v.getColorHex() != null ? v.getColorHex() : "#E8E5DF"));
            }
        }

        // Measurements grouped by garment section for the size chart
        Map<String, List<ProductMeasurement>> measurementsBySection = product.getMeasurements().stream()
                .collect(Collectors.groupingBy(ProductMeasurement::getGarmentSection, LinkedHashMap::new, Collectors.toList()));
        List<SizeChartSection> sizeChart = buildSizeChart(measurementsBySection);

        List<ProductReview> reviews = reviewRepo
                .findByProductIdAndStatusOrderByCreatedAtDesc(product.getId(), "approved");
        double avgRating = reviewRepo.averageRating(product.getId());
        long reviewCount = reviewRepo.countByProductIdAndStatus(product.getId(), "approved");

        String primaryImage = product.getImages().stream()
                .filter(ProductImage::getIsPrimary)
                .map(ProductImage::getImageUrl)
                .findFirst()
                .orElseGet(() -> product.getImages().stream()
                        .map(ProductImage::getImageUrl)
                        .findFirst()
                        .orElse("/images/placeholder.svg"));

        boolean anyAvailable = variantViews.stream().anyMatch(VariantView::isAvailableNow);

        return new ProductDetail(product, variantViews, colors, measurementsBySection, sizeChart,
                reviews, avgRating, reviewCount, primaryImage, anyAvailable);
    }

    /**
     * Turns raw measurement rows into a renderable chart:
     * one block per garment section, one column per size, one row per
     * measurement name (values aligned to the size columns).
     */
    private List<SizeChartSection> buildSizeChart(Map<String, List<ProductMeasurement>> bySection) {
        List<SizeChartSection> sections = new ArrayList<>();
        for (Map.Entry<String, List<ProductMeasurement>> entry : bySection.entrySet()) {
            List<Long> sizeOrder = new ArrayList<>();
            for (ProductMeasurement m : entry.getValue()) {
                if (!sizeOrder.contains(m.getSize().getId())) {
                    sizeOrder.add(m.getSize().getId());
                }
            }
            List<String> sizeNames = sizeOrder.stream()
                    .map(id -> entry.getValue().stream()
                            .filter(m -> m.getSize().getId().equals(id))
                            .findFirst()
                            .map(m -> m.getSize().getSizeName())
                            .orElse("?"))
                    .toList();

            Map<String, Map<Long, String>> rows = new LinkedHashMap<>();
            for (ProductMeasurement m : entry.getValue()) {
                rows.computeIfAbsent(m.getMeasurementName(), k -> new LinkedHashMap<>())
                        .put(m.getSize().getId(),
                                m.getMeasurementValue().stripTrailingZeros().toPlainString()
                                        + " " + m.getUnit());
            }
            List<SizeChartRow> chartRows = rows.entrySet().stream()
                    .map(r -> new SizeChartRow(r.getKey(),
                            sizeOrder.stream()
                                    .map(id -> r.getValue().getOrDefault(id, "-"))
                                    .toList()))
                    .toList();
            sections.add(new SizeChartSection(entry.getKey(), sizeNames, chartRows));
        }
        return sections;
    }

    // ------------------------------------------------------------- mapping

    private List<ProductCard> toCards(List<Product> products) {
        List<Long> ids = products.stream().map(Product::getId).toList();
        Map<Long, Integer> availableByProduct = availablePerProduct(products);

        List<ProductCard> cards = new ArrayList<>();
        for (Product p : products) {
            int available = availableByProduct.getOrDefault(p.getId(), 0);
            cards.add(ProductCard.builder()
                    .id(p.getId())
                    .productCode(p.getProductCode())
                    .name(p.getName())
                    .brandName(p.getBrand() != null ? p.getBrand().getName() : null)
                    .categoryName(p.getCategory() != null ? p.getCategory().getName() : null)
                    .price(p.getDefaultSellingPrice())
                    .imageUrl(primaryImageOf(p))
                    .availableUnits(available)
                    .inStock(available > 0)
                    .build());
        }
        return cards;
    }

    /** Sums available-to-sell across every variant of each product. */
    private Map<Long, Integer> availablePerProduct(List<Product> products) {
        Map<Long, Long> variantToProduct = new HashMap<>();
        for (Product p : products) {
            for (ProductVariant v : p.getVariants()) {
                if (v.getStatus() == ProductVariantStatus.active) {
                    variantToProduct.put(v.getId(), p.getId());
                }
            }
        }
        if (variantToProduct.isEmpty()) {
            return Map.of();
        }
        Map<Long, Integer> available = inventoryService.availableByVariant(variantToProduct.keySet());
        Map<Long, Integer> perProduct = new HashMap<>();
        available.forEach((variantId, units) ->
                perProduct.merge(variantToProduct.get(variantId), units, Integer::sum));
        return perProduct;
    }

    private String primaryImageOf(Product p) {
        return p.getImages().stream()
                .filter(ProductImage::getIsPrimary)
                .map(ProductImage::getImageUrl)
                .findFirst()
                .orElseGet(() -> p.getImages().stream()
                        .map(ProductImage::getImageUrl)
                        .findFirst()
                        .orElse("/images/placeholder.svg"));
    }

    /** Everything the product page needs in one view object. */
    public record ProductDetail(
            Product product,
            List<VariantView> variants,
            List<VariantColor> colors,
            Map<String, List<ProductMeasurement>> measurementsBySection,
            List<SizeChartSection> sizeChart,
            List<ProductReview> reviews,
            double averageRating,
            long reviewCount,
            String primaryImageUrl,
            boolean anyAvailable) {
    }

    /** One colour offered by the product, with its swatch hex. */
    public record VariantColor(Long id, String name, String hex) {}

    /** One block of the size chart: a garment section with size columns. */
    public record SizeChartSection(String section, List<String> sizes, List<SizeChartRow> rows) {}

    /** One row of a size chart: measurement name plus one value per size column. */
    public record SizeChartRow(String name, List<String> values) {}
}
