package io.github.ngtrphuc.smartphone_shop.controller.api.v1;

import java.util.List;
import java.util.regex.Pattern;

import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import io.github.ngtrphuc.smartphone_shop.api.dto.*;
import io.github.ngtrphuc.smartphone_shop.api.ApiMapper;
import io.github.ngtrphuc.smartphone_shop.model.CartItem;
import io.github.ngtrphuc.smartphone_shop.model.Order;
import io.github.ngtrphuc.smartphone_shop.model.PaymentMethod;
import io.github.ngtrphuc.smartphone_shop.model.User;
import io.github.ngtrphuc.smartphone_shop.repository.UserRepository;
import io.github.ngtrphuc.smartphone_shop.service.CartService;
import io.github.ngtrphuc.smartphone_shop.service.OrderService;
import io.github.ngtrphuc.smartphone_shop.service.PaymentMethodService;
import jakarta.servlet.http.HttpSession;

@RestController
@RequestMapping("/api/v1/profile")
public class ProfileApiController {

    private static final Pattern PHONE_PATTERN = Pattern.compile("^[0-9+()\\-\\s]{6,30}$");

    private final UserRepository userRepository;
    private final OrderService orderService;
    private final CartService cartService;
    private final PaymentMethodService paymentMethodService;
    private final ApiMapper apiMapper;

    public ProfileApiController(UserRepository userRepository,
            OrderService orderService,
            CartService cartService,
            PaymentMethodService paymentMethodService,
            ApiMapper apiMapper) {
        this.userRepository = userRepository;
        this.orderService = orderService;
        this.cartService = cartService;
        this.paymentMethodService = paymentMethodService;
        this.apiMapper = apiMapper;
    }

    @GetMapping
    public ProfileResponse profile(Authentication authentication, HttpSession session) {
        return currentProfile(authentication, session);
    }

    @PutMapping
    public ProfileResponse update(@RequestBody UpdateProfileRequest request,
            Authentication authentication,
            HttpSession session) {
        String normalizedFullName = normalizeRequiredField(
                request.fullName(), "Full name cannot be empty.", "Full name is too long.", 100);
        String normalizedPhoneNumber = normalizeOptionalField(
                request.phoneNumber(), "Phone number is too long.", 30);
        String normalizedAddress = normalizeOptionalField(
                request.defaultAddress(), "Address is too long.", 200);

        if (normalizedPhoneNumber != null && !PHONE_PATTERN.matcher(normalizedPhoneNumber).matches()) {
            throw new IllegalArgumentException("Phone number format is invalid.");
        }

        User user = userRepository.findByEmailIgnoreCase(authentication.getName())
                .orElseThrow(() -> new IllegalArgumentException("User not found."));
        user.setFullName(normalizedFullName);
        user.setPhoneNumber(normalizedPhoneNumber);
        user.setDefaultAddress(normalizedAddress);
        userRepository.save(user);
        return currentProfile(authentication, session);
    }

    private ProfileResponse currentProfile(Authentication authentication, HttpSession session) {
        String email = authentication.getName();
        User user = userRepository.findByEmailIgnoreCase(email)
                .orElseThrow(() -> new IllegalArgumentException("User not found."));
        List<Order> orders = orderService.getOrdersByUser(email);
        List<CartItem> cartItems = cartService.getCart(email, session);
        List<PaymentMethod> paymentMethods = paymentMethodService.getUserPaymentMethods(email);
        return apiMapper.toProfileResponse(user, orders, cartItems, paymentMethods);
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

    private record UpdateProfileRequest(String fullName, String phoneNumber, String defaultAddress) {
    }
}

