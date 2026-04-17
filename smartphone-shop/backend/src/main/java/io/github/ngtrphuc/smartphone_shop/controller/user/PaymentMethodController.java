package io.github.ngtrphuc.smartphone_shop.controller.user;

import java.util.Locale;

import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import io.github.ngtrphuc.smartphone_shop.common.exception.BusinessException;
import io.github.ngtrphuc.smartphone_shop.model.PaymentMethod;
import io.github.ngtrphuc.smartphone_shop.service.PaymentMethodService;

@Controller
@RequestMapping("/profile/payment-methods")
public class PaymentMethodController {

    private final PaymentMethodService paymentMethodService;

    public PaymentMethodController(PaymentMethodService paymentMethodService) {
        this.paymentMethodService = paymentMethodService;
    }

    @PostMapping("/add")
    public String add(@RequestParam(name = "type") String type,
            @RequestParam(name = "bankDetail", required = false) String bankDetail,
            @RequestParam(name = "setAsDefault", defaultValue = "false") boolean setAsDefault,
            Authentication auth,
            RedirectAttributes redirectAttributes) {
        try {
            PaymentMethod.Type paymentType = PaymentMethod.Type.valueOf(type.trim().toUpperCase(Locale.ROOT));
            paymentMethodService.addPaymentMethod(auth.getName(), paymentType, bankDetail, setAsDefault);
            redirectAttributes.addFlashAttribute("toast", "Payment method added.");
        } catch (BusinessException | IllegalArgumentException | IllegalStateException ex) {
            redirectAttributes.addFlashAttribute("toast", ex.getMessage());
        }
        return "redirect:/profile";
    }

    @PostMapping("/{id}/set-default")
    public String setDefault(@PathVariable(name = "id") Long id, Authentication auth, RedirectAttributes redirectAttributes) {
        try {
            paymentMethodService.setDefault(auth.getName(), id);
            redirectAttributes.addFlashAttribute("toast", "Default payment method updated.");
        } catch (BusinessException ex) {
            redirectAttributes.addFlashAttribute("toast", ex.getMessage());
        }
        return "redirect:/profile";
    }

    @PostMapping("/{id}/remove")
    public String remove(@PathVariable(name = "id") Long id, Authentication auth, RedirectAttributes redirectAttributes) {
        try {
            paymentMethodService.remove(auth.getName(), id);
            redirectAttributes.addFlashAttribute("toast", "Payment method removed.");
        } catch (BusinessException ex) {
            redirectAttributes.addFlashAttribute("toast", ex.getMessage());
        }
        return "redirect:/profile";
    }
}
