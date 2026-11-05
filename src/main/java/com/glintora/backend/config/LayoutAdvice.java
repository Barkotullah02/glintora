package com.glintora.backend.config;

import com.glintora.backend.model.Brand;
import com.glintora.backend.model.Category;
import com.glintora.backend.repository.BrandRepository;import com.glintora.backend.repository.CategoryRepository;
import com.glintora.backend.repository.CustomerRepository;
import com.glintora.backend.service.CartService;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;
import java.util.List;

/** Makes header/footer data available to every storefront/admin template. */
@ControllerAdvice
@RequiredArgsConstructor
public class LayoutAdvice {

    private final CartService cartService;
    private final CategoryRepository categoryRepo;
    private final BrandRepository brandRepo;

    @ModelAttribute
    public void addGlobalModel(Model model, HttpSession session) {
        model.addAttribute("cartCount", cartService.itemCount(session));
        model.addAttribute("navCategories", categoryRepo.findByParentCategoryIsNullOrderByNameAsc());
        model.addAttribute("navBrands", brandRepo.findAllByOrderByNameAsc());
        model.addAttribute("womenId", categoryRepo.findByName("Women").map(Category::getId).orElse(null));
        model.addAttribute("menId", categoryRepo.findByName("Men").map(Category::getId).orElse(null));
    }
}
