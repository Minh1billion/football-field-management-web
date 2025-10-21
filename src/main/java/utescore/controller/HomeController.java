package utescore.controller;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
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
                        model.addAttribute("account", account);
                        model.addAttribute("role", account.getRole().name());
                        if (account.getRole().name().equals("ADMIN")) {
                            return "redirect:/admin/dashboard";
                        }
                        return "home/user-home";
                    }
                }
            } catch (Exception e) {
                // Ignored - fallback below
            }
        }
        return "home/public-home";
    }

    @GetMapping("/logout")
    public String logout(HttpServletResponse response) {
        Cookie cookie = new Cookie("token", null);
        cookie.setMaxAge(0);
        cookie.setPath("/");
        response.addCookie(cookie);

        return "redirect:/home/public-home";
    }

    @GetMapping("/home/public-home")
    public String publicHome() {
        return "home/public-home";
    }
}
