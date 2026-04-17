package io.github.ngtrphuc.smartphone_shop.controller.user;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.ui.ExtendedModelMap;
import org.springframework.ui.Model;

import io.github.ngtrphuc.smartphone_shop.common.exception.ValidationException;
import io.github.ngtrphuc.smartphone_shop.service.AuthService;

@ExtendWith(MockitoExtension.class)
class AuthControllerTest {

    @Mock
    private AuthService authService;

    private AuthController authController;

    @BeforeEach
    void setUp() {
        authController = new AuthController(authService);
    }

    @Test
    void register_shouldRenderFormWhenValidationFails() {
        when(authService.register("not-an-email", "Tester", "secret123"))
                .thenThrow(new ValidationException("Please enter a valid email address."));

        Model model = new ExtendedModelMap();
        String view = authController.register("not-an-email", "Tester", "secret123", model);

        assertEquals("auth/register", view);
        assertEquals("Please enter a valid email address.", model.getAttribute("error"));
        assertEquals("not-an-email", model.getAttribute("email"));
        assertEquals("Tester", model.getAttribute("fullName"));
    }
}
