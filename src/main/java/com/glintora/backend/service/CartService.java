package com.glintora.backend.service;

import com.glintora.backend.dto.CartLine;
import com.glintora.backend.model.Product;
import com.glintora.backend.model.ProductVariant;
import com.glintora.backend.model.enums.ProductStatus;
import com.glintora.backend.model.enums.ProductVariantStatus;
import com.glintora.backend.repository.ProductVariantRepository;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.math.BigDecimal;
import java.util.*;

/**
 * Session-based shopping cart. The cart lives only in the browser session so
 * guests can shop without an account; stock is validated and reserved when
 * the order is placed at checkout (see {@link OrderService}).
 */
@Service
@RequiredArgsConstructor
public class CartService {

    public static final String CART_ATTR = "GLINTORA_CART";
    public static final int MAX_QTY_PER_LINE = 20;

    private final ProductVariantRepository variantRepo;
    private final InventoryService inventoryService;

    private Map<Long, Integer> cartMap(HttpSession session) {
        @SuppressWarnings("unchecked")
        Map<Long, Integer> cart = (Map<Long, Integer>) session.getAttribute(CART_ATTR);
        if (cart == null) {
            cart = new LinkedHashMap<>();
            session.setAttribute(CART_ATTR, cart);
        }
        return cart;
    }

    /**
     * Add a variant to the cart. The requested quantity is clamped to the
     * currently available-to-sell quantity; the backend re-validates
     * atomically at checkout.
     */
    @Transactional(readOnly = true)
    public void add(HttpSession session, Long variantId, int quantity) {
        if (quantity < 1) {
            quantity = 1;
        }
        ProductVariant variant = loadSellableVariant(variantId);
        int available = inventoryService.available(variantId);
        int alreadyInCart = cartMap(session).getOrDefault(variantId, 0);
        int remaining = Math.max(0, available - alreadyInCart);
        int toAdd = Math.min(quantity, Math.min(remaining, MAX_QTY_PER_LINE));
        if (toAdd <= 0) {
            throw new InsufficientStockException("Sorry, '" + label(variant) + "' is out of stock.");
        }
        cartMap(session).merge(variantId, toAdd, Integer::sum);
    }

    public void updateQuantity(HttpSession session, Long variantId, int quantity) {
        Map<Long, Integer> cart = cartMap(session);
        if (!cart.containsKey(variantId)) {
            return;
        }
        if (quantity <= 0) {
            cart.remove(variantId);
        } else {
            cart.put(variantId, Math.min(quantity, MAX_QTY_PER_LINE));
        }
    }

    public void remove(HttpSession session, Long variantId) {
        cartMap(session).remove(variantId);
    }

    public void clear(HttpSession session) {
        session.removeAttribute(CART_ATTR);
    }

    public int itemCount(HttpSession session) {
        return cartMap(session).values().stream().mapToInt(Integer::intValue).sum();
    }

    /** Resolves the raw cart map into displayable lines with live prices. */
    @Transactional(readOnly = true)
    public List<CartLine> lines(HttpSession session) {
        Map<Long, Integer> cart = cartMap(session);
        if (cart.isEmpty()) {
            return List.of();
        }
        Map<Long, Integer> available = inventoryService.availableByVariant(cart.keySet());
        List<CartLine> lines = new ArrayList<>();
        List<Long> staleIds = new ArrayList<>();
        for (Map.Entry<Long, Integer> entry : cart.entrySet()) {
            Optional<ProductVariant> found = variantRepo.findById(entry.getKey());
            if (found.isEmpty() || !isSellable(found.get())) {
                staleIds.add(entry.getKey()); // drop stale/hidden variants after iteration
                continue;
            }
            ProductVariant variant = found.get();
            Product product = variant.getProduct();
            int avail = available.getOrDefault(variant.getId(), 0);
            lines.add(CartLine.builder()
                    .variantId(variant.getId())
                    .sku(variant.getSku())
                    .productId(product.getId())
                    .productCode(product.getProductCode())
                    .productName(product.getName())
                    .sizeName(variant.getSize() != null ? variant.getSize().getSizeName() : null)
                    .colorName(variant.getColor() != null ? variant.getColor().getColorName() : null)
                    .imageUrl(primaryImage(product))
                    .unitPrice(variant.effectiveSellingPrice())
                    .quantity(entry.getValue())
                    .available(avail)
                    .inStock(avail > 0)
                    .build());
        }
        staleIds.forEach(cart::remove);
        return lines;
    }

    @Transactional(readOnly = true)
    public BigDecimal subtotal(HttpSession session) {
        return lines(session).stream()
                .map(CartLine::lineTotal)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    // ------------------------------------------------------------ internals

    private ProductVariant loadSellableVariant(Long variantId) {
        ProductVariant variant = variantRepo.findById(variantId)
                .orElseThrow(() -> new InvalidOperationException("That product option is no longer available."));
        if (!isSellable(variant)) {
            throw new InvalidOperationException("That product option is no longer available.");
        }
        return variant;
    }

    private boolean isSellable(ProductVariant variant) {
        return variant.getStatus() == ProductVariantStatus.active
                && variant.getProduct() != null
                && variant.getProduct().getStatus() == ProductStatus.published;
    }

    private String primaryImage(Product product) {
        return product.getImages().stream()
                .filter(com.glintora.backend.model.ProductImage::getIsPrimary)
                .map(com.glintora.backend.model.ProductImage::getImageUrl)
                .findFirst()
                .orElseGet(() -> product.getImages().stream()
                        .map(com.glintora.backend.model.ProductImage::getImageUrl)
                        .findFirst()
                        .orElse("/images/placeholder.svg"));
    }

    private String label(ProductVariant variant) {
        Product product = variant.getProduct();
        String base = product != null ? product.getName() : variant.getSku();
        if (variant.getSize() != null) {
            base += " (" + variant.getSize().getSizeName() + ")";
        }
        return base;
    }
}
