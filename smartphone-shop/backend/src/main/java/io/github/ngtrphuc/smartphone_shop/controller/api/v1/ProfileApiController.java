package io.github.ngtrphuc.smartphone_shop.controller.api.v1;

import java.util.List;

import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import io.github.ngtrphuc.smartphone_shop.api.dto.*;
import io.github.ngtrphuc.smartphone_shop.api.ApiMapper;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import io.github.ngtrphuc.smartphone_shop.model.PaymentMethod;
import io.github.ngtrphuc.smartphone_shop.model.User;
import io.github.ngtrphuc.smartphone_shop.service.CartService;
import io.github.ngtrphuc.smartphone_shop.service.OrderService;
import io.github.ngtrphuc.smartphone_shop.service.PaymentMethodService;
import io.github.ngtrphuc.smartphone_shop.service.ProfileService;

@RestController
@RequestMapping("/api/v1/profile")
public class ProfileApiController {

    private final ProfileService profileService;
    private final OrderService orderService;
    private final CartService cartService;
    private final PaymentMethodService paymentMethodService;
    private final ApiMapper apiMapper;

    public ProfileApiController(ProfileService profileService,
            OrderService orderService,
            CartService cartService,
            PaymentMethodService paymentMethodService,
            ApiMapper apiMapper) {
        this.profileService = profileService;
        this.orderService = orderService;
        this.cartService = cartService;
        this.paymentMethodService = paymentMethodService;
        this.apiMapper = apiMapper;
    }

    @GetMapping
    public ProfileResponse profile(Authentication authentication) {
        return currentProfile(authentication);
    }

    @PutMapping
    public ProfileResponse update(@Valid @RequestBody UpdateProfileRequest request,
            Authentication authentication) {
        profileService.updateProfile(
                authentication.getName(),
                request.fullName(),
                request.phoneNumber(),
                request.defaultAddress());
        return currentProfile(authentication);
    }

    private ProfileResponse currentProfile(Authentication authentication) {
        String email = authentication.getName();
        User user = profileService.findUserByEmail(email);
        long deliveredOrderCount = orderService.countDeliveredOrdersByUser(email);
        long pendingOrderCount = orderService.countPendingOrdersByUser(email);
        int cartItemCount = cartService.countUserCartItems(email);
        List<PaymentMethod> paymentMethods = paymentMethodService.getUserPaymentMethods(email);
        return apiMapper.toProfileResponse(
                user,
                deliveredOrderCount,
                pendingOrderCount,
                cartItemCount,
                paymentMethods);
    }

    private record UpdateProfileRequest(
            @NotBlank(message = "Full name cannot be empty.")
            @Size(max = 100, message = "Full name is too long.")
            String fullName,
            @Size(max = 30, message = "Phone number is too long.")
            String phoneNumber,
            @Size(max = 200, message = "Address is too long.")
            String defaultAddress) {
    }
}

