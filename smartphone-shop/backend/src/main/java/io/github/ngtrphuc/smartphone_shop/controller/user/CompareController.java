package io.github.ngtrphuc.smartphone_shop.controller.user;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import io.github.ngtrphuc.smartphone_shop.model.Product;
import io.github.ngtrphuc.smartphone_shop.repository.ProductRepository;
import io.github.ngtrphuc.smartphone_shop.service.CompareService;
import jakarta.servlet.http.HttpSession;

@Controller
@RequestMapping("/compare")
public class CompareController {

    private final ProductRepository productRepository;
    private final CompareService compareService;

    public CompareController(ProductRepository productRepository, CompareService compareService) {
        this.productRepository = productRepository;
        this.compareService = compareService;
    }

    @GetMapping
    public String comparePage(Authentication auth, HttpSession session, Model model) {
        String email = getEmail(auth);
        List<Long> ids = compareService.getCompareIds(email, session);
        List<Product> ordered = resolveOrderedProducts(ids);

        if (ordered.size() != ids.size()) {
            List<Long> cleanedIds = ordered.stream()
                    .map(Product::getId)
                    .filter(Objects::nonNull)
                    .toList();
            compareService.saveCompareIds(email, session, cleanedIds);
            ids = cleanedIds;
        }

        model.addAttribute("products", ordered);
        Set<Long> selectedIds = new java.util.HashSet<>(ids);
        List<Product> availableProducts = ids.isEmpty()
                ? productRepository.findAllByOrderByNameAsc()
                : productRepository.findByIdNotInOrderByNameAsc(ids);
        availableProducts = availableProducts.stream()
                .filter(product -> product.getId() != null && !selectedIds.contains(product.getId()))
                .toList();
        model.addAttribute("availableProducts", availableProducts);
        return "compare";
    }

    @PostMapping("/add")
    public String add(
            @RequestParam long id,
            @RequestParam(required = false) String redirect,
            Authentication auth,
            HttpSession session,
            RedirectAttributes redirectAttributes) {
        CompareService.AddResult result = compareService.addItem(getEmail(auth), session, id);
        String toast = switch (result) {
            case ADDED -> "Added to compare list.";
            case ALREADY_EXISTS -> "This product is already in compare list.";
            case LIMIT_REACHED -> "You can compare up to " + compareService.getMaxCompare() + " products.";
            case UNAVAILABLE -> "Product not found.";
        };
        redirectAttributes.addFlashAttribute("toast", toast);
        return "redirect:" + normalizeRedirect(redirect);
    }

    @PostMapping("/remove")
    public String remove(
            @RequestParam long id,
            @RequestParam(required = false) String redirect,
            Authentication auth,
            HttpSession session) {
        compareService.removeItem(getEmail(auth), session, id);
        return "redirect:" + normalizeRedirect(redirect);
    }

    @PostMapping("/clear")
    public String clear(Authentication auth, HttpSession session) {
        compareService.clear(getEmail(auth), session);
        return "redirect:/compare";
    }

    private List<Product> resolveOrderedProducts(List<Long> ids) {
        if (ids == null || ids.isEmpty()) {
            return List.of();
        }
        List<Product> products = productRepository.findAllByIdIn(ids);
        Map<Long, Product> productsById = new HashMap<>();
        for (Product product : products) {
            if (product != null && product.getId() != null) {
                productsById.put(product.getId(), product);
            }
        }
        return ids.stream()
                .map(productsById::get)
                .filter(Objects::nonNull)
                .toList();
    }

    private String getEmail(Authentication auth) {
        if (auth == null) {
            return null;
        }
        String name = auth.getName();
        if (name == null || name.isBlank() || "anonymousUser".equals(name)) {
            return null;
        }
        return name;
    }

    private String normalizeRedirect(String redirect) {
        if (redirect == null || redirect.isBlank() || !redirect.startsWith("/") || redirect.startsWith("//")) {
            return "/compare";
        }
        return redirect.trim();
    }
}
