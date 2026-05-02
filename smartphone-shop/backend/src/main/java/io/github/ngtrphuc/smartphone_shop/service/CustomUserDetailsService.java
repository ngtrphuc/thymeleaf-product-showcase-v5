package io.github.ngtrphuc.smartphone_shop.service;

import java.util.List;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import io.github.ngtrphuc.smartphone_shop.model.AccountStatus;
import io.github.ngtrphuc.smartphone_shop.repository.UserRepository;

@Service
public class CustomUserDetailsService implements UserDetailsService {

    private final UserRepository userRepository;
    private final boolean requireEmailVerification;

    public CustomUserDetailsService(UserRepository userRepository,
            @Value("${app.auth.require-email-verification:false}") boolean requireEmailVerification) {
        this.userRepository = userRepository;
        this.requireEmailVerification = requireEmailVerification;
    }

    @Override
    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
        String normalizedEmail = email == null ? "" : email.trim();
        return userRepository.findByEmailIgnoreCase(normalizedEmail)
                .map(user -> {
                    if (user.getAccountStatus() != AccountStatus.ACTIVE) {
                        throw new DisabledException("Your account is not active.");
                    }
                    if (requireEmailVerification && !user.isEmailVerified()) {
                        throw new DisabledException("Please verify your email before signing in.");
                    }
                    return new org.springframework.security.core.userdetails.User(
                            user.getEmail(),
                            user.getPassword(),
                            List.of(new SimpleGrantedAuthority(user.getRoleName())));
                })
                .orElseThrow(() -> new UsernameNotFoundException("User not found: " + normalizedEmail));
    }
}
