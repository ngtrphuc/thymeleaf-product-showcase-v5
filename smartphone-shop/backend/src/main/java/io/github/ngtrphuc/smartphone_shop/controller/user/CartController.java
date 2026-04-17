package io.github.ngtrphuc.smartphone_shop.controller.user;

import java.util.List;
import java.util.Locale;
import java.util.regex.Pattern;

import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import io.github.ngtrphuc.smartphone_shop.model.CartItem;
import io.github.ngtrphuc.smartphone_shop.model.PaymentMethod;
import io.github.ngtrphuc.smartphone_shop.repository.UserRepository;
import io.github.ngtrphuc.smartphone_shop.support.StorefrontSupport;
import io.github.ngtrphuc.smartphone_shop.service.CartService;
import io.github.ngtrphuc.smartphone_shop.service.OrderService;
import io.github.ngtrphuc.smartphone_shop.service.OrderValidationException;
import io.github.ngtrphuc.smartphone_shop.service.PaymentMethodService;
import jakarta.servlet.http.HttpSession;

@Controller
@RequestMapping("/cart")
public class CartController {

    private static final Pattern PHONE_PATTERN = Pattern.compile("^[0-9+()\\-\\s]{6,30}$");
    private static final int MAX_NAME_LENGTH = 120;
    private static final int MAX_PHONE_LENGTH = 30;
    private static final int MAX_ADDRESS_LENGTH = 255;
    private static final int MAX_BANK_DETAIL_LENGTH = 200;
    private static final List<String> SUPPORTED_PAYMENT_METHODS = List.of(
            "CASH_ON_DELIVERY", "BANK_TRANSFER", "PAYPAY", "MASTERCARD");
    private static final List<String> SUPPORTED_PAYMENT_PLANS = List.of(
            "FULL_PAYMENT", "INSTALLMENT");
    private static final int DEFAULT_INSTALLMENT_MONTHS = 24;
    private static final List<Integer> SUPPORTED_INSTALLMENT_MONTHS = List.of(6, 12, DEFAULT_INSTALLMENT_MONTHS);

    private final CartService cartService;
    private final OrderService orderService;
    private final UserRepository userRepository;
    private final PaymentMethodService paymentMethodService;

    public CartController(CartService cartService, OrderService orderService,
            UserRepository userRepository, PaymentMethodService paymentMethodService) {
        this.cartService = cartService;
        this.orderService = orderService;
        this.userRepository = userRepository;
        this.paymentMethodService = paymentMethodService;
    }

    private String getEmail(Authentication auth) {
        return auth != null ? auth.getName() : null;
    }

    private boolean isAuthenticatedUser(String email) {
        return email != null && !"anonymousUser".equals(email);
    }

    @GetMapping
    public String viewCart(Authentication auth, HttpSession session, Model model) {
        String email = getEmail(auth);
        List<CartItem> cart = cartService.getCart(email, session);
        cartService.syncCartCount(session, email);
        model.addAttribute("cart", cart);
        model.addAttribute("totalAmount", cartService.calculateTotal(cart));
        return "cart";
    }

    @PostMapping("/add")
    public String add(@RequestParam(name = "id") long id,
            @RequestParam(name = "quantity", defaultValue = "1") int quantity,
            @RequestParam(name = "mode", defaultValue = "cart") String mode,
            Authentication auth,
            HttpSession session, RedirectAttributes redirectAttributes) {
        int safeQuantity = Math.max(1, quantity);
        CartService.AddItemResult result = cartService.addItem(getEmail(auth), session, id, safeQuantity);
        cartService.syncCartCount(session, getEmail(auth));
        String toast = switch (result) {
            case ADDED -> "Added to cart successfully.";
            case LIMIT_REACHED -> "You've already added the maximum available stock for this product.";
            case UNAVAILABLE -> "This product is unavailable right now.";
        };
        if ("buy".equalsIgnoreCase(mode) && result != CartService.AddItemResult.UNAVAILABLE) {
            return "redirect:/cart/payment";
        }
        redirectAttributes.addFlashAttribute("toast", toast);
        return "redirect:/product/" + id;
    }

    @PostMapping("/increase/{id}")
    public String increase(@PathVariable(name = "id") long id, Authentication auth, HttpSession session) {
        cartService.increaseItem(getEmail(auth), session, id);
        cartService.syncCartCount(session, getEmail(auth));
        return "redirect:/cart";
    }

    @PostMapping("/decrease/{id}")
    public String decrease(@PathVariable(name = "id") long id, Authentication auth, HttpSession session) {
        cartService.decreaseItem(getEmail(auth), session, id);
        cartService.syncCartCount(session, getEmail(auth));
        return "redirect:/cart";
    }

    @PostMapping("/remove/{id}")
    public String remove(@PathVariable(name = "id") long id, Authentication auth, HttpSession session) {
        cartService.removeItem(getEmail(auth), session, id);
        cartService.syncCartCount(session, getEmail(auth));
        return "redirect:/cart";
    }

    @GetMapping("/payment")
    public String paymentPage(Authentication auth, HttpSession session, Model model) {
        String email = getEmail(auth);
        List<CartItem> cart = cartService.getCart(email, session);
        if (cart.isEmpty()) {
            return "redirect:/cart";
        }
        String selectedPaymentPlan = resolvePaymentPlan(session.getAttribute("paymentPlan"));
        model.addAttribute("totalAmount", cartService.calculateTotal(cart));
        model.addAttribute("selectedPaymentPlan", selectedPaymentPlan);
        model.addAttribute("selectedInstallmentMonths", resolveInstallmentMonths(
                selectedPaymentPlan,
                session.getAttribute("installmentMonths")));
        model.addAttribute("installmentOptions", SUPPORTED_INSTALLMENT_MONTHS);

        if (isAuthenticatedUser(email)) {
            List<PaymentMethod> savedPaymentMethods = paymentMethodService.getUserPaymentMethods(email);
            model.addAttribute("savedPaymentMethods", savedPaymentMethods);
            model.addAttribute("selectedDefaultPaymentType", savedPaymentMethods.stream()
                    .filter(PaymentMethod::isDefault)
                    .map(PaymentMethod::getType)
                    .map(Enum::name)
                    .findFirst()
                    .orElse(null));
        } else {
            model.addAttribute("savedPaymentMethods", List.of());
            model.addAttribute("selectedDefaultPaymentType", null);
        }
        return "payment-select";
    }

    @PostMapping("/select-payment")
    public String selectPayment(@RequestParam(name = "paymentType", required = false) String paymentType,
            @RequestParam(name = "savedPaymentMethodId", required = false) Long savedPaymentMethodId,
            @RequestParam(name = "bankDetail", required = false) String bankDetail,
            @RequestParam(name = "paymentPlan", required = false) String paymentPlan,
            @RequestParam(name = "installmentMonths", required = false) String installmentMonths,
            @RequestParam(name = "paymentChoice", required = false) String paymentChoice,
            @RequestParam(name = "paymentMethodRadio", required = false) String paymentMethodRadio,
            @RequestParam(name = "paymentPlanChoice", required = false) String paymentPlanChoice,
            @RequestParam(name = "paymentPlanRadio", required = false) String paymentPlanRadio,
            Authentication auth,
            HttpSession session,
            RedirectAttributes redirectAttributes) {
        String email = getEmail(auth);
        if (cartService.getCart(email, session).isEmpty()) {
            return "redirect:/cart";
        }

        String submittedPaymentChoice = firstNonBlank(paymentChoice, paymentMethodRadio);
        Long resolvedSavedPaymentMethodId = savedPaymentMethodId != null
                ? savedPaymentMethodId
                : parseSavedPaymentMethodId(submittedPaymentChoice);
        String resolvedPaymentType = firstNonBlank(
                paymentType,
                parseSubmittedPaymentType(submittedPaymentChoice));

        if (resolvedSavedPaymentMethodId == null
                && (resolvedPaymentType == null || resolvedPaymentType.isBlank())) {
            redirectAttributes.addFlashAttribute("toast", "Please select a payment method.");
            return "redirect:/cart/payment";
        }

        String submittedPaymentPlan = firstNonBlank(paymentPlan, paymentPlanChoice, paymentPlanRadio);
        String finalMethod;
        String finalDetail = null;
        String finalPaymentPlan = resolvePaymentPlan(normalizeSubmittedPaymentPlan(submittedPaymentPlan));
        String resolvedInstallmentMonths = firstNonBlank(
                installmentMonths,
                extractInstallmentMonths(submittedPaymentPlan));
        Integer finalInstallmentMonths = resolveInstallmentMonths(finalPaymentPlan, resolvedInstallmentMonths);

        if (resolvedSavedPaymentMethodId != null) {
            if (!isAuthenticatedUser(email)) {
                redirectAttributes.addFlashAttribute("toast", "Please log in to use saved payment methods.");
                return "redirect:/login";
            }
            PaymentMethod savedMethod = paymentMethodService.getUserPaymentMethods(email).stream()
                    .filter(pm -> resolvedSavedPaymentMethodId.equals(pm.getId()))
                    .findFirst()
                    .orElse(null);
            if (savedMethod == null) {
                redirectAttributes.addFlashAttribute("toast", "Invalid payment method selected.");
                return "redirect:/cart/payment";
            }
            finalMethod = savedMethod.getType().name();
            if (!SUPPORTED_PAYMENT_METHODS.contains(finalMethod)) {
                redirectAttributes.addFlashAttribute("toast", "This payment method is no longer supported.");
                return "redirect:/cart/payment";
            }
            finalDetail = savedMethod.getDetail();
        } else {
            finalMethod = resolvedPaymentType.trim().toUpperCase(Locale.ROOT);

            if (!SUPPORTED_PAYMENT_METHODS.contains(finalMethod)) {
                redirectAttributes.addFlashAttribute("toast", "Invalid payment method selected.");
                return "redirect:/cart/payment";
            }
            if ("BANK_TRANSFER".equals(finalMethod)) {
                String normalizedDetail = normalizeInline(bankDetail);
                if (normalizedDetail.isBlank()) {
                    redirectAttributes.addFlashAttribute("toast", "Please enter your bank account details.");
                    return "redirect:/cart/payment";
                }
                if (normalizedDetail.length() > MAX_BANK_DETAIL_LENGTH) {
                    redirectAttributes.addFlashAttribute("toast", "Bank account details are too long.");
                    return "redirect:/cart/payment";
                }
                finalDetail = normalizedDetail;
            }
        }

        if ("INSTALLMENT".equals(finalPaymentPlan) && "CASH_ON_DELIVERY".equals(finalMethod)) {
            redirectAttributes.addFlashAttribute("toast",
                    "Installment is not available for Cash on Delivery. Please choose another payment method.");
            return "redirect:/cart/payment";
        }

        session.setAttribute("paymentMethod", finalMethod);
        session.setAttribute("paymentDetail", finalDetail);
        session.setAttribute("paymentPlan", finalPaymentPlan);
        if (finalInstallmentMonths != null) {
            session.setAttribute("installmentMonths", finalInstallmentMonths);
        } else {
            session.removeAttribute("installmentMonths");
        }
        return "redirect:/cart/shipping";
    }

    @GetMapping("/shipping")
    public String shipping(Authentication auth, HttpSession session, Model model) {
        String email = getEmail(auth);
        if (cartService.getCart(email, session).isEmpty()) {
            return "redirect:/cart";
        }
        if (session.getAttribute("paymentMethod") == null) {
            return "redirect:/cart/payment";
        }
        if (isAuthenticatedUser(email)) {
            userRepository.findByEmailIgnoreCase(email).ifPresent(u -> {
                model.addAttribute("user", u);
                String savedAddress = normalizeInline(u.getDefaultAddress());
                if (!savedAddress.isBlank()) {
                    model.addAttribute("savedAddress", savedAddress);
                }
            });
        }
        return "shipping";
    }

    @PostMapping("/process-shipping")
    public String processShipping(@RequestParam(name = "customerName") String customerName,
            @RequestParam(name = "phoneNumber") String phoneNumber,
            @RequestParam(name = "addressOption", required = false) String addressOption,
            @RequestParam(name = "savedAddress", required = false) String savedAddress,
            @RequestParam(name = "address", required = false) String address,
            HttpSession session,
            RedirectAttributes redirectAttributes) {
        String normalizedName = normalizeInline(customerName);
        String normalizedPhone = normalizeInline(phoneNumber);
        String normalizedSavedAddress = normalizeInline(savedAddress);
        String normalizedAddress = normalizeInline(address);
        String finalAddress = "new".equals(addressOption) ? normalizedAddress
                : (!normalizedSavedAddress.isBlank() ? normalizedSavedAddress : normalizedAddress);

        if (normalizedName.isBlank() || normalizedPhone.isBlank() || finalAddress.isBlank()) {
            redirectAttributes.addFlashAttribute("toast", "Please complete your shipping details.");
            return "redirect:/cart/shipping";
        }
        if (normalizedName.length() > MAX_NAME_LENGTH) {
            redirectAttributes.addFlashAttribute("toast", "Full name is too long.");
            return "redirect:/cart/shipping";
        }
        if (normalizedPhone.length() > MAX_PHONE_LENGTH || !PHONE_PATTERN.matcher(normalizedPhone).matches()) {
            redirectAttributes.addFlashAttribute("toast", "Phone number format is invalid.");
            return "redirect:/cart/shipping";
        }
        if (finalAddress.length() > MAX_ADDRESS_LENGTH) {
            redirectAttributes.addFlashAttribute("toast", "Shipping address is too long.");
            return "redirect:/cart/shipping";
        }

        session.setAttribute("name", normalizedName);
        session.setAttribute("phone", normalizedPhone);
        session.setAttribute("address", finalAddress);
        return "redirect:/cart/checkout";
    }

    @GetMapping("/checkout")
    public String checkout(Authentication auth, HttpSession session, Model model) {
        String email = getEmail(auth);
        List<CartItem> cart = cartService.getCart(email, session);
        if (cart.isEmpty()) {
            return "redirect:/cart";
        }
        String paymentMethod = (String) session.getAttribute("paymentMethod");
        if (paymentMethod == null) {
            return "redirect:/cart/payment";
        }

        model.addAttribute("cart", cart);
        double totalAmount = cartService.calculateTotal(cart);
        String paymentPlan = resolvePaymentPlan(session.getAttribute("paymentPlan"));
        Integer resolvedInstallmentMonths = resolveInstallmentMonths(paymentPlan, session.getAttribute("installmentMonths"));
        Long installmentMonthlyAmount = "INSTALLMENT".equals(paymentPlan)
                ? Math.round(totalAmount / resolvedInstallmentMonths)
                : null;

        model.addAttribute("totalAmount", totalAmount);
        model.addAttribute("count", cart.stream().mapToInt(CartItem::getQuantity).sum());
        model.addAttribute("paymentMethodDisplay", resolvePaymentDisplay(
                paymentMethod,
                (String) session.getAttribute("paymentDetail")));
        model.addAttribute("paymentPlan", paymentPlan);
        model.addAttribute("paymentPlanDisplay",
                "INSTALLMENT".equals(paymentPlan) ? "Installment plan" : "Full payment");
        model.addAttribute("isInstallment", "INSTALLMENT".equals(paymentPlan));
        model.addAttribute("installmentMonths", resolvedInstallmentMonths);
        model.addAttribute("installmentMonthlyAmount", installmentMonthlyAmount);
        return "checkout";
    }

    @PostMapping("/confirm")
    public String confirm(Authentication auth, HttpSession session, RedirectAttributes redirectAttributes) {
        String name = (String) session.getAttribute("name");
        String phone = (String) session.getAttribute("phone");
        String address = (String) session.getAttribute("address");
        String paymentMethod = (String) session.getAttribute("paymentMethod");
        String paymentDetail = (String) session.getAttribute("paymentDetail");
        String paymentPlan = resolvePaymentPlan(session.getAttribute("paymentPlan"));
        Integer installmentMonths = resolveInstallmentMonths(paymentPlan, session.getAttribute("installmentMonths"));
        String email = getEmail(auth);
        List<CartItem> cart = cartService.getCart(email, session);

        if (!isAuthenticatedUser(email)) {
            redirectAttributes.addFlashAttribute("toast", "Please log in before placing an order.");
            return "redirect:/login";
        }
        if (cart.isEmpty() || name == null || phone == null || address == null) {
            return "redirect:/cart/shipping";
        }
        if (paymentMethod == null) {
            return "redirect:/cart/payment";
        }

        try {
            orderService.createOrder(
                    email, name, phone, address, cart,
                    paymentMethod, paymentDetail, paymentPlan, installmentMonths);
            cartService.clearCart(email, session);
            session.removeAttribute("name");
            session.removeAttribute("phone");
            session.removeAttribute("address");
            session.removeAttribute("paymentMethod");
            session.removeAttribute("paymentDetail");
            session.removeAttribute("paymentPlan");
            session.removeAttribute("installmentMonths");

            redirectAttributes.addFlashAttribute("orderSuccess", true);
            return "redirect:/cart/success";
        } catch (OrderValidationException ex) {
            redirectAttributes.addFlashAttribute("toast", ex.getMessage());
            return "redirect:/cart/checkout";
        }
    }

    @GetMapping("/success")
    public String success() {
        return "success";
    }

    private String normalizeInline(String value) {
        return value == null ? "" : value.trim().replaceAll("\\s+", " ");
    }

    private String resolvePaymentDisplay(String method, String detail) {
        return StorefrontSupport.paymentDisplayName(method, detail);
    }

    private String resolvePaymentPlan(Object rawPlan) {
        String normalized = rawPlan == null ? "" : rawPlan.toString().trim().toUpperCase(Locale.ROOT);
        if (SUPPORTED_PAYMENT_PLANS.contains(normalized)) {
            return normalized;
        }
        return "FULL_PAYMENT";
    }

    private Integer resolveInstallmentMonths(String paymentPlan, Object rawMonths) {
        if (!"INSTALLMENT".equals(paymentPlan)) {
            return null;
        }
        Integer parsed = parseInstallmentMonths(rawMonths);
        if (parsed == null || !SUPPORTED_INSTALLMENT_MONTHS.contains(parsed)) {
            return DEFAULT_INSTALLMENT_MONTHS;
        }
        return parsed;
    }

    private Integer parseInstallmentMonths(Object rawMonths) {
        if (rawMonths == null) {
            return null;
        }
        if (rawMonths instanceof Integer value) {
            return value;
        }
        try {
            return Integer.valueOf(rawMonths.toString().trim());
        } catch (NumberFormatException ex) {
            return null;
        }
    }

    private String firstNonBlank(String... values) {
        if (values == null) {
            return null;
        }
        for (String value : values) {
            if (value != null && !value.isBlank()) {
                return value;
            }
        }
        return null;
    }

    private String normalizeSubmittedPaymentPlan(String rawPlan) {
        if (rawPlan == null || rawPlan.isBlank()) {
            return null;
        }
        String normalized = rawPlan.trim().toUpperCase(Locale.ROOT);
        if (normalized.startsWith("INSTALLMENT:")) {
            return "INSTALLMENT";
        }
        return normalized;
    }

    private String extractInstallmentMonths(String rawPlan) {
        if (rawPlan == null || rawPlan.isBlank()) {
            return null;
        }
        String normalized = rawPlan.trim().toUpperCase(Locale.ROOT);
        if (!normalized.startsWith("INSTALLMENT:")) {
            return null;
        }
        return normalized.substring("INSTALLMENT:".length()).trim();
    }

    private Long parseSavedPaymentMethodId(String rawChoice) {
        if (rawChoice == null || rawChoice.isBlank()) {
            return null;
        }
        if (!rawChoice.startsWith("saved:")) {
            return null;
        }
        try {
            return Long.valueOf(rawChoice.substring("saved:".length()).trim());
        } catch (NumberFormatException ex) {
            return null;
        }
    }

    private String parseSubmittedPaymentType(String rawChoice) {
        if (rawChoice == null || rawChoice.isBlank()) {
            return null;
        }
        if (!rawChoice.startsWith("new:")) {
            return null;
        }
        return rawChoice.substring("new:".length()).trim();
    }
}
