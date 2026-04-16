package io.github.ngtrphuc.smartphone_shop.config;
import java.io.IOException;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationSuccessHandler;
import org.springframework.stereotype.Component;
import io.github.ngtrphuc.smartphone_shop.service.CartService;
import io.github.ngtrphuc.smartphone_shop.service.CompareService;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
@Component
public class LoginSuccessHandler extends SimpleUrlAuthenticationSuccessHandler {
    private final CartService cartService;
    private final CompareService compareService;
    public LoginSuccessHandler(CartService cartService, CompareService compareService) {
        this.cartService = cartService;
        this.compareService = compareService;
        setDefaultTargetUrl("/");
        setAlwaysUseDefaultTargetUrl(true);
    }

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request,
            HttpServletResponse response,
            Authentication authentication) throws IOException, ServletException {
        HttpSession session = request.getSession();
        String email = authentication.getName();
        cartService.mergeSessionCartToDb(session, email);
        cartService.syncCartCount(session, email);
        compareService.mergeSessionCompareToDb(session, email);
        super.onAuthenticationSuccess(request, response, authentication);
    }
}
