package io.github.ngtrphuc.smartphone_shop.controller.api.v1;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import io.github.ngtrphuc.smartphone_shop.api.ApiDtos;
import io.github.ngtrphuc.smartphone_shop.api.ApiMapper;
import io.github.ngtrphuc.smartphone_shop.model.User;
import io.github.ngtrphuc.smartphone_shop.repository.UserRepository;
import io.github.ngtrphuc.smartphone_shop.service.AuthService;

@RestController
@RequestMapping("/api/v1/auth")
public class AuthApiController {

    private final AuthService authService;
    private final UserRepository userRepository;
    private final ApiMapper apiMapper;

    public AuthApiController(AuthService authService, UserRepository userRepository, ApiMapper apiMapper) {
        this.authService = authService;
        this.userRepository = userRepository;
        this.apiMapper = apiMapper;
    }

    @GetMapping("/me")
    public ApiDtos.AuthMeResponse me(Authentication authentication) {
        if (!isAuthenticatedUser(authentication)) {
            return new ApiDtos.AuthMeResponse(false, null, null, null);
        }
        User user = userRepository.findByEmailIgnoreCase(authentication.getName()).orElse(null);
        return apiMapper.toAuthMeResponse(user);
    }

    @PostMapping("/register")
    public ResponseEntity<ApiDtos.OperationStatusResponse> register(@RequestBody RegisterRequest request) {
        boolean success = authService.register(request.email(), request.fullName(), request.password());
        if (!success) {
            return ResponseEntity.status(HttpStatus.CONFLICT)
                    .body(new ApiDtos.OperationStatusResponse(false, "Email already exists."));
        }
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(new ApiDtos.OperationStatusResponse(true, "Registration successful."));
    }

    private boolean isAuthenticatedUser(Authentication authentication) {
        return authentication != null
                && authentication.isAuthenticated()
                && !(authentication instanceof AnonymousAuthenticationToken);
    }

    record RegisterRequest(String email, String fullName, String password) {
    }
}
