package utescore.controller;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.CookieValue;
import org.springframework.web.bind.annotation.GetMapping;
import utescore.entity.Account;
import utescore.service.AccountService;
import utescore.service.JwtService;

@Controller
public class HomeController {

    private final AccountService accountService;
    private final JwtService jwtService;

    public HomeController(AccountService accountService, JwtService jwtService) {
        this.accountService = accountService;
        this.jwtService = jwtService;
    }

    @GetMapping({"/", "/home"})
    public String home(@CookieValue(value = "token", required = false) String token, Model model) {
        if (token != null) {
            try {
                if (jwtService.isTokenValid(token)) {
                    String username = jwtService.extractUsername(token);
                    Account account = accountService.findByUsername(username).orElse(null);
                    if (account != null) {
                        // Điều hướng theo vai trò
                        switch (account.getRole()) {
                            case MANAGER:
                                return "redirect:/manager/fields";
                            case ADMIN:
                                // Nếu có dashboard admin, điều hướng vào đây
                                return "redirect:/admin/dashboard";
                            case USER:
                            default:
                                model.addAttribute("account", account);
                                model.addAttribute("role", account.getRole().name());
                                return "home/user-home";
                        }
                    }
                }
            } catch (Exception e) {
                // bỏ qua, rơi xuống public-home
            }
        }
        return "home/public-home";
    }

}