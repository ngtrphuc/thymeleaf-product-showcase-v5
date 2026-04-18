package io.github.ngtrphuc.smartphone_shop.controller.api.v1;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.ResponseCookie;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import io.github.ngtrphuc.smartphone_shop.api.dto.*;
import io.github.ngtrphuc.smartphone_shop.api.ApiMapper;
import io.github.ngtrphuc.smartphone_shop.model.User;
import io.github.ngtrphuc.smartphone_shop.repository.UserRepository;
import io.github.ngtrphuc.smartphone_shop.security.JwtTokenProvider;
import io.github.ngtrphuc.smartphone_shop.service.AuthService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

@RestController
@RequestMapping("/api/v1/auth")
public class AuthApiController {
    private static final String JWT_COOKIE_NAME = "jwt";

    private final AuthService authService;
    private final UserRepository userRepository;
    private final ApiMapper apiMapper;
    private final AuthenticationManager authenticationManager;
    private final JwtTokenProvider jwtTokenProvider;

    public AuthApiController(AuthService authService,
            UserRepository userRepository,
            ApiMapper apiMapper,
            AuthenticationManager authenticationManager,
            JwtTokenProvider jwtTokenProvider) {
        this.authService = authService;
        this.userRepository = userRepository;
        this.apiMapper = apiMapper;
        this.authenticationManager = authenticationManager;
        this.jwtTokenProvider = jwtTokenProvider;
    }

    @GetMapping("/me")
    public AuthMeResponse me(Authentication authentication) {
        if (!isAuthenticatedUser(authentication)) {
            return new AuthMeResponse(false, null, null, null);
        }
        User user = userRepository.findByEmailIgnoreCase(authentication.getName()).orElse(null);
        return apiMapper.toAuthMeResponse(user);
    }

    @PostMapping("/login")
    public AuthTokenResponse login(@RequestBody LoginRequest request,
            HttpServletRequest httpRequest,
            HttpServletResponse httpResponse) {
        if (request.email() == null || request.email().isBlank()
                || request.password() == null || request.password().isBlank()) {
            throw new IllegalArgumentException("Email and password are required.");
        }
        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.email(), request.password()));
        User user = userRepository.findByEmailIgnoreCase(authentication.getName())
                .orElseThrow(() -> new IllegalArgumentException("User not found."));
        String token = jwtTokenProvider.generateAccessToken(user.getEmail(), user.getRole());
        long expiresInSeconds = jwtTokenProvider.getExpiresInSeconds(token);
        httpResponse.addHeader("Set-Cookie", buildJwtCookie(token, expiresInSeconds, httpRequest.isSecure()).toString());
        return new AuthTokenResponse(
                token,
                "Bearer",
                expiresInSeconds,
                user.getEmail(),
                user.getRole(),
                user.getFullName());
    }

    @PostMapping("/logout")
    public OperationStatusResponse logout(HttpServletRequest httpRequest, HttpServletResponse httpResponse) {
        httpResponse.addHeader("Set-Cookie", buildJwtCookie("", 0, httpRequest.isSecure()).toString());
        return new OperationStatusResponse(true, "Logged out successfully.");
    }

    @PostMapping("/register")
    public ResponseEntity<OperationStatusResponse> register(@RequestBody RegisterRequest request) {
        boolean success = authService.register(request.email(), request.fullName(), request.password());
        if (!success) {
            return ResponseEntity.status(HttpStatus.CONFLICT)
                    .body(new OperationStatusResponse(false, "Email already exists."));
        }
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(new OperationStatusResponse(true, "Registration successful."));
    }

    private boolean isAuthenticatedUser(Authentication authentication) {
        return authentication != null
                && authentication.isAuthenticated()
                && !(authentication instanceof AnonymousAuthenticationToken);
    }

    record LoginRequest(String email, String password) {
    }

    record RegisterRequest(String email, String fullName, String password) {
    }

    private ResponseCookie buildJwtCookie(String token, long maxAgeSeconds, boolean secureRequest) {
        return ResponseCookie.from(JWT_COOKIE_NAME, token)
                .httpOnly(true)
                .secure(secureRequest)
                .sameSite("Lax")
                .path("/")
                .maxAge(Math.max(maxAgeSeconds, 0))
                .build();
    }
}

