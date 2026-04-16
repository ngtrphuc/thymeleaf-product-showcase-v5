package io.github.ngtrphuc.smartphone_shop.controller.user;

import java.util.List;

import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import io.github.ngtrphuc.smartphone_shop.model.WishlistItem;
import io.github.ngtrphuc.smartphone_shop.service.WishlistService;

@Controller
@RequestMapping("/wishlist")
public class WishlistController {

    private final WishlistService wishlistService;

    public WishlistController(WishlistService wishlistService) {
        this.wishlistService = wishlistService;
    }

    @GetMapping
    public String wishlist(Authentication auth, Model model) {
        String email = auth.getName();
        List<WishlistItem> wishlist = wishlistService.getWishlist(email);
        model.addAttribute("wishlist", wishlist);
        return "wishlist";
    }

    @PostMapping("/add")
    public String add(@RequestParam long id,
            @RequestParam(required = false) String redirect,
            Authentication auth,
            RedirectAttributes redirectAttributes) {
        WishlistService.AddResult result = wishlistService.addItem(auth.getName(), id);
        String toast = switch (result) {
            case ADDED -> "Added to wishlist.";
            case ALREADY_EXISTS -> "This product is already in your wishlist.";
            case UNAVAILABLE -> "This product is unavailable right now.";
        };
        redirectAttributes.addFlashAttribute("toast", toast);
        return "redirect:" + normalizeRedirectPath(redirect);
    }

    @PostMapping("/remove")
    public String remove(@RequestParam long id,
            @RequestParam(required = false) String redirect,
            Authentication auth,
            RedirectAttributes redirectAttributes) {
        boolean removed = wishlistService.removeItem(auth.getName(), id);
        redirectAttributes.addFlashAttribute("toast", removed
                ? "Removed from wishlist."
                : "This product is not in your wishlist.");
        return "redirect:" + normalizeRedirectPath(redirect);
    }

    private String normalizeRedirectPath(String redirect) {
        if (redirect == null || redirect.isBlank()) {
            return "/wishlist";
        }
        String trimmed = redirect.trim();
        if (!trimmed.startsWith("/") || trimmed.startsWith("//")) {
            return "/wishlist";
        }
        if (trimmed.contains("\r") || trimmed.contains("\n")) {
            return "/wishlist";
        }
        return trimmed;
    }
}
