package utescore.controller;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.CookieValue;
import org.springframework.web.bind.annotation.GetMapping;
import utescore.entity.Account;
import utescore.service.AccountService;
import utescore.service.JwtService;

@Controller
@RequiredArgsConstructor
public class HomeController {

    private final AccountService accountService;
    private final JwtService jwtService;

    @GetMapping({"/", "/home"})
    public String home(@CookieValue(value = "token", required = false) String token, Model model) {
        if (token != null) {
            try {
                if (jwtService.isTokenValid(token)) {
                    String username = jwtService.extractUsername(token);
                    Account account = accountService.findByUsername(username).orElse(null);

                    if (account != null) {
                        // Điều hướng theo vai trò người dùng
                        switch (account.getRole()) {
                            case MANAGER:
                                return "redirect:/manager/dashboard";
                            case ADMIN:
                                return "redirect:/admin/dashboard";
                            case USER:
                                return "redirect:/user/user-home";
                            default:
                                model.addAttribute("account", account);
                                model.addAttribute("role", account.getRole().name());
                                return "home/public-home";
                        }
                    }
                }
            } catch (Exception e) {
                // Nếu có lỗi khi xác thực token, quay về trang public
            }
        }
        return "home/public-home";
    }

    @GetMapping("/logout")
    public String logout(HttpServletResponse response, HttpServletRequest request) {
        // Xóa cookie token
        Cookie cookie = new Cookie("token", null);
        cookie.setMaxAge(0);
        cookie.setPath("/");
        cookie.setHttpOnly(true);
        cookie.setSecure(false);
        response.addCookie(cookie);

        // Xóa session và SecurityContext
        request.getSession().invalidate();
        SecurityContextHolder.clearContext();

        return "redirect:/home";
    }

    @GetMapping("/home/public-home")
    public String publicHome() {
        return "home/public-home";
    }
    

    @GetMapping("/maintenance")
    public String showMaintenancePage() {
         return "home/maintenance";
    }

}
