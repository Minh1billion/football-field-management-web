package utescore.config;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.AuthorityUtils;
import org.springframework.security.web.authentication.SavedRequestAwareAuthenticationSuccessHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.Set;

@Component
public class CustomAuthenticationSuccessHandler extends SavedRequestAwareAuthenticationSuccessHandler {

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response,
                                        Authentication authentication) throws IOException, ServletException {

        Set<String> roles = AuthorityUtils.authorityListToSet(authentication.getAuthorities());

        String targetUrl = determineTargetUrl(roles);
        if (response.isCommitted()) {
            return;
        }

        super.onAuthenticationSuccess(request, response, authentication);

        if (!response.isCommitted()) {
            getRedirectStrategy().sendRedirect(request, response, targetUrl);
        }
    }

    private String determineTargetUrl(Set<String> roles) {
        if (roles.contains("ROLE_ADMIN")) {
            return "/admin/dashboard";
        } else if (roles.contains("ROLE_MANAGER")) {
            return "/manager/dashboard";
        } else if (roles.contains("ROLE_USER")) {
            return "/user/dashboard";
        } else {
            return "/";
        }
    }
}
