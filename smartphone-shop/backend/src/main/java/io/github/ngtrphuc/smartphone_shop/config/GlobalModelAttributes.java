package io.github.ngtrphuc.smartphone_shop.config;

import java.util.List;

import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;

import jakarta.servlet.http.HttpSession;

@ControllerAdvice
public class GlobalModelAttributes {

    @ModelAttribute
    public void addCommonAttributes(Model model, HttpSession session) {
        model.addAttribute("shopname", "Smartphone Shop");
        model.addAttribute("address", "Asaka, Saitama, Japan");

        Object compareObject = session.getAttribute("compareIds");
        int compareCount = 0;
        if (compareObject instanceof List<?> list) {
            compareCount = list.size();
        }
        model.addAttribute("compareCount", compareCount);
    }
}
