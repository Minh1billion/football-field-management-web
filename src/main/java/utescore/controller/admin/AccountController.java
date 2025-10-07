package utescore.controller.admin;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import utescore.dto.RegisterRequest;
import utescore.entity.Account;
import utescore.service.AccountService;

@Controller
@RequestMapping("/admin/accounts")
public class AccountController {

    @Autowired
    private AccountService accountService;

    @GetMapping
    public String listAccounts(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) String email,
            @RequestParam(required = false) String role,
            Model model) {
        Pageable pageable = PageRequest.of(page, size);
        Page<Account> accountPage;
        
        if ((email != null && !email.isEmpty()) || (role != null && !role.isEmpty())) {
            accountPage = accountService.findByEmailAndRole(email, role, pageable);
        } else {
            accountPage = accountService.findAll(pageable);
        }
        
        model.addAttribute("accounts", accountPage);
        model.addAttribute("currentPage", page);
        model.addAttribute("totalPages", accountPage.getTotalPages());
        model.addAttribute("searchEmail", email);
        model.addAttribute("searchRole", role);
        model.addAttribute("roles", Account.Role.values());
        return "admin/accounts/list";
    }
    
    @GetMapping("/create")
    public String showCreateForm(Model model) {
        model.addAttribute("registerRequest", new RegisterRequest());
        model.addAttribute("roles", Account.Role.values());
        return "admin/accounts/create";
    }

    @PostMapping("/create")
    public String createAccount(
            @ModelAttribute RegisterRequest registerRequest,
            @RequestParam String role,
            RedirectAttributes redirectAttributes) {
        try {
            accountService.createAccountByRole(registerRequest, role);
            redirectAttributes.addFlashAttribute("successMessage", "Tạo tài khoản thành công!");
        } catch (IllegalArgumentException e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
            return "redirect:/admin/accounts/create";
        }
        return "redirect:/admin/accounts";
    }

    @GetMapping("/edit/{id}")
    public String showEditForm(@PathVariable Long id, Model model) {
        Account account = accountService.findById(id);
        if (account == null) {
            return "redirect:/admin/accounts";
        }
        model.addAttribute("account", account);
        model.addAttribute("roles", Account.Role.values());
        return "admin/accounts/edit";
    }

    @PostMapping("/edit/{id}")
    public String updateAccount(
            @PathVariable Long id,
            @ModelAttribute Account account,
            RedirectAttributes redirectAttributes) {
        try {
            account.setId(id);
            accountService.updateAccount(account);
            redirectAttributes.addFlashAttribute("successMessage", "Cập nhật tài khoản thành công!");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", "Có lỗi xảy ra khi cập nhật tài khoản!");
        }
        return "redirect:/admin/accounts";
    }

    @PostMapping("/delete/{id}")
    public String deleteAccount(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        try {
            accountService.deleteById(id);
            redirectAttributes.addFlashAttribute("successMessage", "Xóa tài khoản thành công!");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", "Không thể xóa tài khoản này!");
        }
        return "redirect:/admin/accounts";
    }

    @GetMapping("/view/{id}")
    public String viewAccount(@PathVariable Long id, Model model) {
        Account account = accountService.findById(id);
        if (account == null) {
            return "redirect:/admin/accounts";
        }
        model.addAttribute("account", account);
        return "admin/accounts/view";
    }
}