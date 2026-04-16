package io.github.ngtrphuc.smartphone_shop.controller.user;

import java.util.List;
import java.util.regex.Pattern;

import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import io.github.ngtrphuc.smartphone_shop.model.CartItem;
import io.github.ngtrphuc.smartphone_shop.model.Order;
import io.github.ngtrphuc.smartphone_shop.model.PaymentMethod;
import io.github.ngtrphuc.smartphone_shop.model.User;
import io.github.ngtrphuc.smartphone_shop.repository.UserRepository;
import io.github.ngtrphuc.smartphone_shop.service.CartService;
import io.github.ngtrphuc.smartphone_shop.service.OrderService;
import io.github.ngtrphuc.smartphone_shop.service.PaymentMethodService;
import jakarta.servlet.http.HttpSession;

@Controller
@RequestMapping("/profile")
public class ProfileController {
    private static final Pattern PHONE_PATTERN = Pattern.compile("^[0-9+()\\-\\s]{6,30}$");

    private final UserRepository userRepository;
    private final OrderService orderService;
    private final CartService cartService;
    private final PaymentMethodService paymentMethodService;

    public ProfileController(UserRepository userRepository, OrderService orderService,
            CartService cartService, PaymentMethodService paymentMethodService) {
        this.userRepository = userRepository;
        this.orderService = orderService;
        this.cartService = cartService;
        this.paymentMethodService = paymentMethodService;
    }

    @GetMapping
    public String profile(Authentication auth, HttpSession session, Model model) {
        String email = auth.getName();
        User user = userRepository.findByEmailIgnoreCase(email).orElseThrow();
        List<Order> allOrders = orderService.getOrdersByUser(email);
        List<Order> deliveredOrders = allOrders.stream()
                .filter(o -> "delivered".equals(o.getStatus())).toList();
        List<Order> pendingOrders = allOrders.stream()
                .filter(o -> !"delivered".equals(o.getStatus())
                && !"cancelled".equals(o.getStatus())).toList();
        List<CartItem> cartItems = cartService.getCart(email, session);
        List<PaymentMethod> paymentMethods = paymentMethodService.getUserPaymentMethods(email);

        model.addAttribute("user", user);
        model.addAttribute("deliveredOrders", deliveredOrders);
        model.addAttribute("pendingOrders", pendingOrders);
        model.addAttribute("cartItems", cartItems);
        model.addAttribute("paymentMethods", paymentMethods);
        return "profile";
    }

    @PostMapping("/update")
    public String updateProfile(Authentication auth,
            @RequestParam String fullName,
            @RequestParam String phoneNumber,
            @RequestParam String defaultAddress,
            RedirectAttributes redirectAttributes) {
        String normalizedFullName;
        String normalizedPhoneNumber;
        String normalizedAddress;
        try {
            normalizedFullName = normalizeRequiredField(
                    fullName, "Full name cannot be empty.", "Full name is too long.", 100);
            normalizedPhoneNumber = normalizeOptionalField(phoneNumber, "Phone number is too long.", 30);
            normalizedAddress = normalizeOptionalField(defaultAddress, "Address is too long.", 200);
        } catch (IllegalArgumentException ex) {
            redirectAttributes.addFlashAttribute("toast", ex.getMessage());
            return "redirect:/profile";
        }
        if (normalizedPhoneNumber != null && !PHONE_PATTERN.matcher(normalizedPhoneNumber).matches()) {
            redirectAttributes.addFlashAttribute("toast", "Phone number format is invalid.");
            return "redirect:/profile";
        }

        boolean updated = userRepository.findByEmailIgnoreCase(auth.getName()).map(user -> {
            user.setFullName(normalizedFullName);
            user.setPhoneNumber(normalizedPhoneNumber);
            user.setDefaultAddress(normalizedAddress);
            userRepository.save(user);
            return true;
        }).orElse(false);
        redirectAttributes.addFlashAttribute("toast", updated ? "Profile updated!" : "Unable to update profile.");
        return "redirect:/profile";
    }

    private String normalizeRequiredField(String value, String emptyMessage, String tooLongMessage, int maxLength) {
        String normalized = normalizeOptionalField(value, tooLongMessage, maxLength);
        if (normalized == null) {
            throw new IllegalArgumentException(emptyMessage);
        }
        return normalized;
    }

    private String normalizeOptionalField(String value, String tooLongMessage, int maxLength) {
        if (value == null) {
            return null;
        }
        String normalized = value.trim().replaceAll("\\s+", " ");
        if (normalized.isBlank()) {
            return null;
        }
        if (normalized.length() > maxLength) {
            throw new IllegalArgumentException(tooLongMessage);
        }
        return normalized;
    }
}

