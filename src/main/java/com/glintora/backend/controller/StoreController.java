package com.glintora.backend.controller;

import com.glintora.backend.dto.CartLine;
import com.glintora.backend.dto.ProductCard;
import com.glintora.backend.model.Customer;
import com.glintora.backend.model.Order;
import com.glintora.backend.repository.BrandRepository;
import com.glintora.backend.repository.CategoryRepository;
import com.glintora.backend.repository.CustomerRepository;
import com.glintora.backend.repository.SizeRepository;
import com.glintora.backend.service.*;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import java.math.BigDecimal;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

/** Public storefront: home, shop, product detail, cart, checkout, auth. */
@Controller
@RequiredArgsConstructor
public class StoreController {

    private final CatalogService catalogService;
    private final CartService cartService;
    private final OrderService orderService;
    private final CustomerService customerService;
    private final BrandRepository brandRepo;
    private final CategoryRepository categoryRepo;
    private final SizeRepository sizeRepo;
    private final CustomerRepository customerRepo;

    private static final BigDecimal FREE_SHIPPING_OVER = new BigDecimal("3000.00");

    // ------------------------------------------------------------- home

    @GetMapping("/")
    public String home(Model model) {
        model.addAttribute("featured", catalogService.featured(4));
        model.addAttribute("newArrivals", catalogService.newArrivals(8));
        model.addAttribute("bestsellers", catalogService.bestsellers(4));
        model.addAttribute("categories", categoryRepo.findAllByOrderByNameAsc());
        model.addAttribute("brands", brandRepo.findAllByOrderByNameAsc());
        return "index";
    }

    // -------------------------------------------------------------- shop

    @GetMapping("/shop")
    public String shop(@RequestParam(required = false) String q,
                       @RequestParam(required = false) Long category,
                       @RequestParam(required = false) Long brand,
                       @RequestParam(required = false) Long size,
                       @RequestParam(required = false, defaultValue = "newest") String sort,
                       Model model) {
        List<ProductCard> products = catalogService.search(q, category, brand, size);
        products = switch (sort) {
            case "price_asc" -> products.stream()
                    .sorted(Comparator.comparing(ProductCard::getPrice,
                            Comparator.nullsLast(Comparator.naturalOrder())))
                    .toList();
            case "price_desc" -> products.stream()
                    .sorted(Comparator.comparing(ProductCard::getPrice,
                            Comparator.nullsLast(Comparator.reverseOrder())))
                    .toList();
            default -> products;
        };
        model.addAttribute("products", products);
        model.addAttribute("categories", categoryRepo.findAllByOrderByNameAsc());
        model.addAttribute("brands", brandRepo.findAllByOrderByNameAsc());
        model.addAttribute("sizes", sizeRepo.findAllByOrderByIdAsc());
        model.addAttribute("q", q);
        model.addAttribute("selectedCategory", category);
        model.addAttribute("selectedBrand", brand);
        model.addAttribute("selectedSize", size);
        model.addAttribute("sort", sort);
        return "shop";
    }

    @GetMapping("/shop/product/{code}")
    public String product(@PathVariable String code, Model model) {
        CatalogService.ProductDetail detail = catalogService.detail(code)
                .orElseThrow(() -> new org.springframework.web.server.ResponseStatusException(
                        org.springframework.http.HttpStatus.NOT_FOUND, "Product not found: " + code));
        model.addAttribute("detail", detail);
        model.addAttribute("product", detail.product());
        model.addAttribute("related", catalogService.related(
                detail.product().getCategory() != null ? detail.product().getCategory().getId() : null,
                detail.product().getId()));
        return "product";
    }

    // -------------------------------------------------------------- cart

    @PostMapping("/cart/add")
    public String addToCart(@RequestParam Long variantId,
                            @RequestParam(defaultValue = "1") int quantity,
                            @RequestParam(required = false) String redirect,
                            HttpSession session, RedirectAttributes ra) {
        try {
            cartService.add(session, variantId, quantity);
            ra.addFlashAttribute("cartMessage", "Added to cart.");
        } catch (InsufficientStockException | InvalidOperationException e) {
            ra.addFlashAttribute("cartError", e.getMessage());
        }
        return "redirect:" + (redirect != null && !redirect.isBlank() ? redirect : "/cart");
    }

    @GetMapping("/cart")
    public String cart(Model model, HttpSession session) {
        List<CartLine> lines = cartService.lines(session);
        BigDecimal subtotal = lines.stream().map(CartLine::lineTotal)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        model.addAttribute("lines", lines);
        model.addAttribute("subtotal", subtotal);
        model.addAttribute("shippingOver", FREE_SHIPPING_OVER);
        return "cart";
    }

    @PostMapping("/cart/update")
    public String updateCart(@RequestParam Long variantId, @RequestParam int quantity,
                             HttpSession session) {
        cartService.updateQuantity(session, variantId, quantity);
        return "redirect:/cart";
    }

    @PostMapping("/cart/remove")
    public String removeFromCart(@RequestParam Long variantId, HttpSession session) {
        cartService.remove(session, variantId);
        return "redirect:/cart";
    }

    // ----------------------------------------------------------- checkout

    @GetMapping("/checkout")
    public String checkout(Model model, HttpSession session, Authentication auth) {
        List<CartLine> lines = cartService.lines(session);
        if (lines.isEmpty()) {
            return "redirect:/cart";
        }
        BigDecimal subtotal = lines.stream().map(CartLine::lineTotal)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        Customer customer = currentCustomer(auth);
        model.addAttribute("lines", lines);
        model.addAttribute("subtotal", subtotal);
        model.addAttribute("shippingFee", OrderService.SHIPPING_FEE);
        model.addAttribute("grandTotal", subtotal.add(OrderService.SHIPPING_FEE));
        model.addAttribute("customer", customer);
        return "checkout";
    }

    @PostMapping("/checkout")
    public String placeOrder(@RequestParam String address,
                             @RequestParam String city,
                             @RequestParam(required = false) String postalCode,
                             @RequestParam(defaultValue = "COD") String paymentMethod,
                             Model model, HttpSession session, Authentication auth,
                             RedirectAttributes ra) {
        Customer customer = currentCustomer(auth);
        Map<Long, Integer> quantities = cartService.lines(session).stream()
                .collect(java.util.stream.Collectors.toMap(
                        CartLine::getVariantId, CartLine::getQuantity, Integer::sum));
        try {
            Order order = orderService.placeOrder(customer, quantities, address, city, postalCode, paymentMethod);
            cartService.clear(session);
            return "redirect:/order/" + order.getId() + "/confirmation";
        } catch (InsufficientStockException | InvalidOperationException e) {
            ra.addFlashAttribute("checkoutError", e.getMessage());
            return "redirect:/checkout";
        }
    }

    @GetMapping("/order/{id}/confirmation")
    public String confirmation(@PathVariable Long id, Model model, Authentication auth) {
        Customer customer = currentCustomer(auth);
        Order order = orderService.getOrderForCustomer(id, customer);
        model.addAttribute("order", order);
        return "confirmation";
    }

    @GetMapping("/orders")
    public String myOrders(Model model, Authentication auth) {
        Customer customer = currentCustomer(auth);
        model.addAttribute("orders", orderService.ordersOf(customer));
        return "my-orders";
    }

    // -------------------------------------------------------------- auth

    @GetMapping("/login")
    public String login() {
        return "login";
    }

    @GetMapping("/register")
    public String register() {
        return "register";
    }

    @PostMapping("/register")
    public String doRegister(@RequestParam String name,
                             @RequestParam String email,
                             @RequestParam(required = false) String phone,
                             @RequestParam String password,
                             RedirectAttributes ra) {
        try {
            customerService.register(name, email, phone, password);
            ra.addFlashAttribute("registered", true);
            return "redirect:/login";
        } catch (InvalidOperationException e) {
            ra.addFlashAttribute("registerError", e.getMessage());
            return "redirect:/register";
        }
    }

    @GetMapping("/wishlist")
    public String wishlist() {
        return "wishlist";
    }

    // ------------------------------------------------------------ helpers

    private Customer currentCustomer(Authentication auth) {
        if (auth == null || !auth.isAuthenticated() || "anonymousUser".equals(auth.getPrincipal())) {
            throw new IllegalStateException("Not signed in");
        }
        return customerRepo.findByEmailIgnoreCase(auth.getName())
                .orElseThrow(() -> new IllegalStateException("Signed-in account not found"));
    }
}
