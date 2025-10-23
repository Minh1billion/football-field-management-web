package utescore.controller.manager;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import utescore.entity.Account;
import utescore.service.AccountService;
import utescore.util.SecurityUtils;

@Controller
@RequestMapping("/manager/profile")
@RequiredArgsConstructor
public class ManagerProfileController {

    private final AccountService accountService;

    @GetMapping
    public String view(Model model) {
        String username = SecurityUtils.getCurrentUsername();
        Account acc = accountService.findByUsername(username).orElse(null);
        model.addAttribute("account", acc);
        return "manager/profile/view";
    }

    @PostMapping("/update-email")
    public String updateEmail(@RequestParam String email) {
        String username = SecurityUtils.getCurrentUsername();
        Account acc = accountService.findByUsername(username).orElseThrow();
        acc.setEmail(email);
        accountService.updateAccount(acc);
        return "redirect:/manager/profile";
    }

    @PostMapping("/change-password")
    public String changePassword(@RequestParam String newPassword) {
        String username = SecurityUtils.getCurrentUsername();
        accountService.changePassword(username, newPassword);
        return "redirect:/manager/profile";
    }
}