package io.github.ngtrphuc.smartphone_shop.controller.api.v1;

import java.util.Objects;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.ResponseCookie;
import org.springframework.lang.NonNull;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import io.github.ngtrphuc.smartphone_shop.api.dto.*;
import io.github.ngtrphuc.smartphone_shop.api.ApiMapper;
import io.github.ngtrphuc.smartphone_shop.model.User;
import io.github.ngtrphuc.smartphone_shop.repository.UserRepository;
import io.github.ngtrphuc.smartphone_shop.security.JwtTokenProvider;
import io.github.ngtrphuc.smartphone_shop.service.AuthService;
import io.github.ngtrphuc.smartphone_shop.service.EmailVerificationService;
import io.github.ngtrphuc.smartphone_shop.service.RefreshTokenService;
import io.github.ngtrphuc.smartphone_shop.service.RefreshTokenService.IssuedRefreshToken;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.Cookie;

@RestController
@RequestMapping("/api/v1/auth")
public class AuthApiController {
    private static final String JWT_COOKIE_NAME = "jwt";
    private static final String REFRESH_COOKIE_NAME = "refresh_token";
    private static final String JWT_COOKIE_PATH = "/";
    private static final String REFRESH_COOKIE_PATH = "/api/v1/auth";

    private final AuthService authService;
    private final UserRepository userRepository;
    private final ApiMapper apiMapper;
    private final AuthenticationManager authenticationManager;
    private final JwtTokenProvider jwtTokenProvider;
    private final EmailVerificationService emailVerificationService;
    private final RefreshTokenService refreshTokenService;
    private final boolean forceSecureJwtCookie;

    public AuthApiController(AuthService authService,
            UserRepository userRepository,
            ApiMapper apiMapper,
            AuthenticationManager authenticationManager,
            JwtTokenProvider jwtTokenProvider,
            EmailVerificationService emailVerificationService,
            RefreshTokenService refreshTokenService,
            @Value("${app.jwt.cookie.secure:false}") boolean forceSecureJwtCookie) {
        this.authService = authService;
        this.userRepository = userRepository;
        this.apiMapper = apiMapper;
        this.authenticationManager = authenticationManager;
        this.jwtTokenProvider = jwtTokenProvider;
        this.emailVerificationService = emailVerificationService;
        this.refreshTokenService = refreshTokenService;
        this.forceSecureJwtCookie = forceSecureJwtCookie;
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
        String userEmail = Objects.requireNonNull(user.getEmail(), "Authenticated user email must not be null.");
        String normalizedRole = Objects.requireNonNull(user.getRoleName(), "Authenticated user role must not be null.");
        String token = Objects.requireNonNull(
                jwtTokenProvider.generateAccessToken(userEmail, normalizedRole),
                "Generated access token must not be null.");
        long expiresInSeconds = jwtTokenProvider.getExpiresInSeconds(token);
        IssuedRefreshToken refreshToken = refreshTokenService.issue(userEmail, httpRequest.getHeader("User-Agent"));
        String refreshTokenValue = Objects.requireNonNull(
                refreshToken.token(),
                "Generated refresh token must not be null.");
        httpResponse.addHeader("Set-Cookie", buildJwtCookie(token, expiresInSeconds, httpRequest.isSecure()).toString());
        httpResponse.addHeader("Set-Cookie", buildRefreshCookie(
                refreshTokenValue, refreshToken.expiresInSeconds(), httpRequest.isSecure()).toString());
        return new AuthTokenResponse(
                token,
                "Bearer",
                expiresInSeconds,
                userEmail,
                normalizedRole,
                user.getFullName());
    }

    @PostMapping("/refresh")
    public AuthTokenResponse refresh(HttpServletRequest httpRequest, HttpServletResponse httpResponse) {
        String rawRefreshToken = resolveCookieValue(httpRequest, REFRESH_COOKIE_NAME);
        var existingRefreshToken = refreshTokenService.validateAndRevoke(rawRefreshToken)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid or expired refresh token."));
        User user = userRepository.findByEmailIgnoreCase(existingRefreshToken.getUserEmail())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "User not found."));

        String userEmail = Objects.requireNonNull(user.getEmail(), "Authenticated user email must not be null.");
        String normalizedRole = Objects.requireNonNull(user.getRoleName(), "Authenticated user role must not be null.");
        String token = Objects.requireNonNull(
                jwtTokenProvider.generateAccessToken(userEmail, normalizedRole),
                "Generated access token must not be null.");
        long expiresInSeconds = jwtTokenProvider.getExpiresInSeconds(token);
        IssuedRefreshToken refreshToken = refreshTokenService.issue(userEmail, httpRequest.getHeader("User-Agent"));
        String refreshTokenValue = Objects.requireNonNull(
                refreshToken.token(),
                "Generated refresh token must not be null.");

        httpResponse.addHeader("Set-Cookie", buildJwtCookie(token, expiresInSeconds, httpRequest.isSecure()).toString());
        httpResponse.addHeader("Set-Cookie", buildRefreshCookie(
                refreshTokenValue, refreshToken.expiresInSeconds(), httpRequest.isSecure()).toString());

        return new AuthTokenResponse(
                token,
                "Bearer",
                expiresInSeconds,
                userEmail,
                normalizedRole,
                user.getFullName());
    }

    @PostMapping("/logout")
    public OperationStatusResponse logout(HttpServletRequest httpRequest, HttpServletResponse httpResponse) {
        refreshTokenService.revoke(resolveCookieValue(httpRequest, REFRESH_COOKIE_NAME));
        httpResponse.addHeader("Set-Cookie", buildJwtCookie("", 0, httpRequest.isSecure()).toString());
        httpResponse.addHeader("Set-Cookie", buildRefreshCookie("", 0, httpRequest.isSecure()).toString());
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

    @PostMapping("/verify-email")
    public OperationStatusResponse verifyEmail(@RequestParam("token") String token) {
        emailVerificationService.verify(token);
        return new OperationStatusResponse(true, "Email verified successfully.");
    }

    @PostMapping("/resend-verification")
    public OperationStatusResponse resendVerification(Authentication authentication) {
        if (!isAuthenticatedUser(authentication)) {
            throw new IllegalArgumentException("Authentication is required.");
        }
        emailVerificationService.resendVerification(authentication.getName());
        return new OperationStatusResponse(true, "Verification email resent.");
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

    @NonNull
    private ResponseCookie buildJwtCookie(@NonNull String token, long maxAgeSeconds, boolean secureRequest) {
        return buildCookie(JWT_COOKIE_NAME, token, maxAgeSeconds, secureRequest, JWT_COOKIE_PATH);
    }

    @NonNull
    private ResponseCookie buildRefreshCookie(@NonNull String token, long maxAgeSeconds, boolean secureRequest) {
        return buildCookie(REFRESH_COOKIE_NAME, token, maxAgeSeconds, secureRequest, REFRESH_COOKIE_PATH);
    }

    @NonNull
    private ResponseCookie buildCookie(@NonNull String name,
            @NonNull String token,
            long maxAgeSeconds,
            boolean secureRequest,
            @NonNull String path) {
        boolean secure = forceSecureJwtCookie || secureRequest;
        return ResponseCookie.from(name, token)
                .httpOnly(true)
                .secure(secure)
                .sameSite("Lax")
                .path(path)
                .maxAge(Math.max(maxAgeSeconds, 0))
                .build();
    }

    private String resolveCookieValue(HttpServletRequest request, String cookieName) {
        Cookie[] cookies = request.getCookies();
        if (cookies == null) {
            return null;
        }
        for (Cookie cookie : cookies) {
            if (cookie == null || cookie.getName() == null) {
                continue;
            }
            if (cookieName.equals(cookie.getName())) {
                String value = cookie.getValue();
                return value == null || value.isBlank() ? null : value.trim();
            }
        }
        return null;
    }
}

