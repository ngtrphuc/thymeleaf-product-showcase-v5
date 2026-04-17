package io.github.ngtrphuc.smartphone_shop.controller.user;

import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import io.github.ngtrphuc.smartphone_shop.service.OrderService;

@Controller
public class OrderController {

    private final OrderService orderService;

    public OrderController(OrderService orderService) {
        this.orderService = orderService;
    }

    @GetMapping("/my-orders")
    public String myOrders(Authentication auth, Model model) {
        model.addAttribute("orders", orderService.getOrdersByUser(auth.getName()));
        return "my-orders";
    }

    @PostMapping("/my-orders/{id}/cancel")
    public String cancelOrder(@PathVariable(name = "id") Long id,
                              Authentication auth,
                              RedirectAttributes ra) {
        boolean success = orderService.cancelOrder(id, auth.getName());
        ra.addFlashAttribute("toast", success
                ? "Order cancelled successfully."
                : "Cannot cancel this order.");
        return "redirect:/my-orders";
    }
}

