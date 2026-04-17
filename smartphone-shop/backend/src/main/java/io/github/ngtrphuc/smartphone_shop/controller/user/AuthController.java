package io.github.ngtrphuc.smartphone_shop.controller.user;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import io.github.ngtrphuc.smartphone_shop.common.exception.BusinessException;
import io.github.ngtrphuc.smartphone_shop.service.AuthService;

@Controller
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @GetMapping("/login")
    public String loginPage(@RequestParam(name = "error", required = false) String error,
                            @RequestParam(name = "logout", required = false) String logout,
                            Model model) {
        if (error != null)  model.addAttribute("error", "Invalid email or password.");
        if (logout != null) model.addAttribute("message", "You have been logged out.");
        return "auth/login";
    }

    @GetMapping("/register")
    public String registerPage() {
        return "auth/register"; 
    }

    @PostMapping("/register")
    public String register(@RequestParam(name = "email") String email, @RequestParam(name = "fullName") String fullName,
                           @RequestParam(name = "password") String password, Model model) {
        boolean success;
        try {
            success = authService.register(email, fullName, password);
        } catch (BusinessException ex) {
            model.addAttribute("error", ex.getMessage());
            model.addAttribute("email", email);
            model.addAttribute("fullName", fullName);
            return "auth/register";
        }
        if (!success) {
            model.addAttribute("error", "Email already exists.");
            model.addAttribute("email", email);
            model.addAttribute("fullName", fullName);
            return "auth/register";
        }
        return "redirect:/login?registered";
    }
}
