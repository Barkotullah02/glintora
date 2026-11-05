package com.glintora.backend.service;

import com.glintora.backend.model.*;
import com.glintora.backend.model.enums.ProductStatus;
import com.glintora.backend.model.enums.ProductVariantStatus;
import com.glintora.backend.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.math.BigDecimal;
import java.util.List;

/** Admin-side product catalogue management (Phase 1 workflows). */
@Service
@RequiredArgsConstructor
public class CatalogAdminService {

    private final ProductRepository productRepo;
    private final ProductVariantRepository variantRepo;
    private final BrandRepository brandRepo;
    private final CategoryRepository categoryRepo;
    private final SizeRepository sizeRepo;
    private final ColorRepository colorRepo;
    private final InventoryMovementRepository movementRepo;

    @Transactional(readOnly = true)
    public List<Product> allProducts() {
        return productRepo.findAllByOrderByCreatedAtDesc();
    }

    @Transactional(readOnly = true)
    public Product getProduct(Long id) {
        return productRepo.findById(id)
                .orElseThrow(() -> new InvalidOperationException("Product not found: " + id));
    }

    /** Create a new draft product with its code, pricing and classification. */
    @Transactional
    public Product createProduct(String productCode, String name, Long brandId, Long categoryId,
                                 String description, String material,
                                 BigDecimal defaultSellingPrice, BigDecimal costPrice) {
        if (productCode == null || productCode.isBlank()) {
            throw new InvalidOperationException("Product code is required.");
        }
        if (productRepo.findByProductCode(productCode.trim()).isPresent()) {
            throw new InvalidOperationException("Product code '" + productCode + "' already exists.");
        }
        Product product = Product.builder()
                .productCode(productCode.trim().toUpperCase())
                .name(name)
                .brand(brandId != null ? brandRepo.findById(brandId).orElse(null) : null)
                .category(categoryId != null ? categoryRepo.findById(categoryId).orElse(null) : null)
                .description(description)
                .material(material)
                .defaultSellingPrice(defaultSellingPrice)
                .costPrice(costPrice)
                .status(ProductStatus.draft)
                .build();
        return productRepo.save(product);
    }

    @Transactional
    public Product updateProduct(Long id, String name, Long brandId, Long categoryId,
                                 String description, String material,
                                 BigDecimal defaultSellingPrice, BigDecimal costPrice) {
        Product product = getProduct(id);
        product.setName(name);
        product.setBrand(brandId != null ? brandRepo.findById(brandId).orElse(null) : null);
        product.setCategory(categoryId != null ? categoryRepo.findById(categoryId).orElse(null) : null);
        product.setDescription(description);
        product.setMaterial(material);
        product.setDefaultSellingPrice(defaultSellingPrice);
        product.setCostPrice(costPrice);
        return productRepo.save(product);
    }

    @Transactional
    public Product changeStatus(Long id, ProductStatus status) {
        Product product = getProduct(id);
        product.setStatus(status);
        product.setUpdatedAt(java.time.LocalDateTime.now());
        return productRepo.save(product);
    }

    /** Add a size/color variant; SKU is auto-generated from code + size + color when blank. */
    @Transactional
    public ProductVariant addVariant(Long productId, String sku, Long sizeId, Long colorId,
                                     BigDecimal sellingPrice, BigDecimal weight) {
        Product product = getProduct(productId);
        String finalSku = (sku == null || sku.isBlank())
                ? buildSku(product, sizeId, colorId)
                : sku.trim().toUpperCase();

        if (variantRepo.findBySku(finalSku).isPresent()) {
            throw new InvalidOperationException("SKU '" + finalSku + "' already exists.");
        }
        boolean duplicate = product.getVariants().stream().anyMatch(v ->
                v.getSku().equals(finalSku)
                        || (sizeId != null && v.getSize() != null && v.getSize().getId().equals(sizeId)
                            && (colorId == null || (v.getColor() != null && v.getColor().getId().equals(colorId)))));
        if (duplicate) {
            throw new InvalidOperationException("That size/color combination already has a variant.");
        }

        ProductVariant variant = ProductVariant.builder()
                .product(product)
                .sku(finalSku)
                .size(sizeId != null ? sizeRepo.findById(sizeId).orElse(null) : null)
                .color(colorId != null ? colorRepo.findById(colorId).orElse(null) : null)
                .sellingPrice(sellingPrice)
                .weight(weight)
                .status(ProductVariantStatus.active)
                .build();
        return variantRepo.save(variant);
    }

    @Transactional
    public void deleteVariant(Long variantId) {
        ProductVariant variant = variantRepo.findById(variantId)
                .orElseThrow(() -> new InvalidOperationException("Variant not found: " + variantId));
        // Guard: never silently drop history-bearing stock
        if (!movementRepo.findByVariantIdOrderByCreatedAtDesc(variantId).isEmpty()) {
            throw new InvalidOperationException(
                    "Variant " + variant.getSku() + " has stock history. Mark it inactive instead of deleting.");
        }
        Product product = variant.getProduct();
        product.getVariants().remove(variant);
        variantRepo.delete(variant);
    }

    @Transactional
    public ProductImage addImage(Long productId, String imageUrl, boolean primary) {
        Product product = getProduct(productId);
        if (primary) {
            product.getImages().forEach(i -> i.setIsPrimary(false));
        }
        ProductImage image = ProductImage.builder()
                .product(product)
                .imageUrl(imageUrl)
                .sortOrder(product.getImages().size())
                .isPrimary(primary)
                .build();
        product.getImages().add(image);
        productRepo.save(product);
        return image;
    }

    private String buildSku(Product product, Long sizeId, Long colorId) {
        StringBuilder sb = new StringBuilder(product.getProductCode());
        if (sizeId != null) {
            sizeRepo.findById(sizeId).ifPresent(s -> sb.append("-").append(abbreviate(s.getSizeName())));
        }
        if (colorId != null) {
            colorRepo.findById(colorId).ifPresent(c -> sb.append("-").append(abbreviate(c.getColorName())));
        }
        return sb.toString();
    }

    private static String abbreviate(String value) {
        String cleaned = value.replaceAll("[^A-Za-z0-9]", "").toUpperCase();
        return cleaned.length() <= 3 ? cleaned : cleaned.substring(0, 3);
    }
}
