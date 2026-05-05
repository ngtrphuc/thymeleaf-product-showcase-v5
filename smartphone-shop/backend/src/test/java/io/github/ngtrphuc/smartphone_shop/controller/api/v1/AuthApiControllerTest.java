package io.github.ngtrphuc.smartphone_shop.controller.api.v1;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.AuthorityUtils;

import io.github.ngtrphuc.smartphone_shop.api.dto.*;
import io.github.ngtrphuc.smartphone_shop.api.ApiMapper;
import io.github.ngtrphuc.smartphone_shop.common.support.AssetUrlResolver;
import io.github.ngtrphuc.smartphone_shop.model.User;
import io.github.ngtrphuc.smartphone_shop.repository.UserRepository;
import io.github.ngtrphuc.smartphone_shop.security.JwtTokenProvider;
import io.github.ngtrphuc.smartphone_shop.service.AuthService;
import io.github.ngtrphuc.smartphone_shop.service.EmailVerificationService;
import io.github.ngtrphuc.smartphone_shop.service.RefreshTokenService;
import io.github.ngtrphuc.smartphone_shop.model.UserRole;

@ExtendWith(MockitoExtension.class)
class AuthApiControllerTest {

    private static final ApiMapper API_MAPPER = new ApiMapper(new AssetUrlResolver(""));

    @Mock
    private AuthService authService;

    @Mock
    private UserRepository userRepository;

    @Mock
    private AuthenticationManager authenticationManager;

    @Mock
    private JwtTokenProvider jwtTokenProvider;

    @Mock
    private EmailVerificationService emailVerificationService;

    @Mock
    private RefreshTokenService refreshTokenService;

    @Test
    void me_shouldReturnAnonymousPayloadWhenNotLoggedIn() {
        AuthApiController controller = new AuthApiController(
                authService, userRepository, API_MAPPER, authenticationManager, jwtTokenProvider,
                emailVerificationService, refreshTokenService, false);

        AuthMeResponse response = controller.me(
                new AnonymousAuthenticationToken("key", "anonymousUser", AuthorityUtils.createAuthorityList("ROLE_ANONYMOUS")));

        assertEquals(false, response.authenticated());
        assertEquals(null, response.email());
    }

    @Test
    void register_shouldReturnConflictWhenEmailExists() {
        AuthApiController controller = new AuthApiController(
                authService, userRepository, API_MAPPER, authenticationManager, jwtTokenProvider,
                emailVerificationService, refreshTokenService, false);
        when(authService.register("user@example.com", "Tester", "secret123")).thenReturn(false);

        ResponseEntity<OperationStatusResponse> response = controller.register(
                new AuthApiController.RegisterRequest("user@example.com", "Tester", "secret123"));

        assertEquals(HttpStatus.CONFLICT, response.getStatusCode());
        OperationStatusResponse body = response.getBody();
        assertNotNull(body);
        assertEquals(false, body.success());
    }

    @Test
    void me_shouldReturnUserPayloadWhenAuthenticated() {
        AuthApiController controller = new AuthApiController(
                authService, userRepository, API_MAPPER, authenticationManager, jwtTokenProvider,
                emailVerificationService, refreshTokenService, false);
        User user = new User();
        user.setEmail("user@example.com");
        user.setFullName("Tester");
        user.setRole(UserRole.ROLE_USER);
        when(userRepository.findByEmailIgnoreCase("user@example.com")).thenReturn(java.util.Optional.of(user));

        AuthMeResponse response = controller.me(
                new UsernamePasswordAuthenticationToken("user@example.com", "password",
                        AuthorityUtils.createAuthorityList("ROLE_USER")));

        assertEquals(true, response.authenticated());
        assertEquals("user@example.com", response.email());
    }

}

