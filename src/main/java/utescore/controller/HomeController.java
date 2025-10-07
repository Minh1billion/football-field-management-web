package utescore.controller;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import utescore.entity.Account;
import utescore.service.AccountService;

@Controller
public class HomeController {

    private final AccountService accountService;

    public HomeController(AccountService accountService) {
        this.accountService = accountService;
    }

    @GetMapping({"/", "/home"})
    public String home(Model model) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        
        if (authentication != null && authentication.isAuthenticated() && 
            !authentication.getName().equals("anonymousUser")) {
            
            Account account = accountService.findByUsername(authentication.getName())
                .orElse(null);
                
            if (account != null) {
                model.addAttribute("account", account);
                model.addAttribute("role", account.getRole().name());
                
                // Role-based content
                switch (account.getRole()) {
                    case ADMIN:
                        return "home/admin-home";
                    case MANAGER:
                        return "home/manager-home";
                    case USER:
                    default:
                        return "home/user-home";
                }
            }
        }
        
        return "home/public-home";
    }
}