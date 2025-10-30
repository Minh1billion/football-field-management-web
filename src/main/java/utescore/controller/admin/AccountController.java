package utescore.controller.admin;

import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import jakarta.validation.Valid;
import utescore.dto.RegisterRequest;
import utescore.entity.Account;
import utescore.service.AccountService;
import utescore.util.WebSocketEventListener;

@Controller
@RequestMapping("/admin/accounts")
public class AccountController {

    @Autowired
    private AccountService accountService;
    
    @GetMapping("/chat")
    public String adminChatPage() {
        return "admin/chat"; // file: templates/admin/chat.html
    }

    private String getCurrentUserRole() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.getPrincipal() instanceof org.springframework.security.core.userdetails.User) {
            org.springframework.security.core.userdetails.User userDetails = 
                (org.springframework.security.core.userdetails.User) authentication.getPrincipal();
            String authority = userDetails.getAuthorities().iterator().next().getAuthority();
            String role = authority.startsWith("ROLE_") ? authority.substring(5) : authority;
            return role;
        }
        return "USER";
    }

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
        model.addAttribute("currentUserRole", getCurrentUserRole());
        return "admin/accounts/list";
    }
    
    @GetMapping("/create")
    @PreAuthorize("hasRole('ADMIN') or hasRole('MANAGER')")
    public String showCreateForm(Model model) {
        model.addAttribute("registerRequest", new RegisterRequest());
        model.addAttribute("roles", Account.Role.values());
        model.addAttribute("currentUserRole", getCurrentUserRole());
        return "admin/accounts/create";
    }

    @PostMapping("/create")
    @PreAuthorize("hasRole('ADMIN') or hasRole('MANAGER')")
    public String createAccount(
            @Valid @ModelAttribute("registerRequest") RegisterRequest registerRequest,
            BindingResult bindingResult,
            @RequestParam String role,
            Model model,
            RedirectAttributes redirectAttributes) {

        // Nếu có lỗi validation → quay lại form
        if (bindingResult.hasErrors()) {
            model.addAttribute("roles", Account.Role.values());
            model.addAttribute("currentUserRole", getCurrentUserRole());
            return "admin/accounts/create";
        }

        // Kiểm tra xác nhận mật khẩu
        if (!registerRequest.getPassword().equals(registerRequest.getConfirmPassword())) {
            model.addAttribute("errorMessage", "Mật khẩu xác nhận không khớp!");
            model.addAttribute("roles", Account.Role.values());
            model.addAttribute("currentUserRole", getCurrentUserRole());
            return "admin/accounts/create";
        }

        try {
            accountService.createAccountByRole(registerRequest, role);
            redirectAttributes.addFlashAttribute("successMessage", "Tạo tài khoản thành công!");
        } catch (IllegalArgumentException e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
            return "redirect:/admin/accounts/create";
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", "Đã xảy ra lỗi khi tạo tài khoản!");
            return "redirect:/admin/accounts/create";
        }

        return "redirect:/admin/accounts";
    }

    @GetMapping("/edit/{id}")
    @PreAuthorize("hasRole('ADMIN') or hasRole('MANAGER')")
    public String showEditForm(@PathVariable Long id, Model model) {
        Account account = accountService.findById(id);
        if (account == null) {
            return "redirect:/admin/accounts";
        }
        model.addAttribute("account", account);
        model.addAttribute("roles", Account.Role.values());
        model.addAttribute("currentUserRole", getCurrentUserRole());
        return "admin/accounts/edit";
    }

    @PostMapping("/edit/{id}")
    @PreAuthorize("hasRole('ADMIN') or hasRole('MANAGER')")
    public String updateAccount(
            @PathVariable Long id,
            @ModelAttribute Account account,
            RedirectAttributes redirectAttributes) {
        try {
            Account existingAccount = accountService.findById(id);
            if (existingAccount == null) {
                redirectAttributes.addFlashAttribute("errorMessage", "Tài khoản không tồn tại!");
                return "redirect:/admin/accounts";
            }
            
            if ((existingAccount.getRole() == Account.Role.ADMIN || existingAccount.getRole() == Account.Role.MANAGER) 
                && account.getRole() == Account.Role.USER) {
                redirectAttributes.addFlashAttribute("errorMessage", "Không thể chuyển Admin/Manager về quyền User!");
                return "redirect:/admin/accounts/edit/" + id;
            }

            account.setId(id);
            account.setPassword(existingAccount.getPassword());
            account.setUsername(existingAccount.getUsername());
            account.setCreatedAt(existingAccount.getCreatedAt());
            
            accountService.updateAccount(account);
            redirectAttributes.addFlashAttribute("successMessage", "Cập nhật tài khoản thành công!");
        } catch (Exception e) {
            System.out.println("ERROR updating account: " + e.getMessage());
            e.printStackTrace();
            redirectAttributes.addFlashAttribute("errorMessage", "Có lỗi xảy ra khi cập nhật tài khoản: " + e.getMessage());
        }
        return "redirect:/admin/accounts";
    }

    @PostMapping("/delete/{id}")
    @PreAuthorize("hasRole('ADMIN') or hasRole('MANAGER')")
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
        model.addAttribute("currentUserRole", getCurrentUserRole());
        return "admin/accounts/view";
    }
    
    @GetMapping("/api/active-users")
    public ResponseEntity<Map<String, Map<String, Object>>> getActiveUsers() {
        return ResponseEntity.ok(WebSocketEventListener.getActiveUsersWithTime());
    }
}