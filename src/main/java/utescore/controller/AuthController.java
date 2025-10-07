package utescore.controller;

import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import utescore.dto.RegisterRequest;
import utescore.dto.OtpVerificationRequest;
import utescore.entity.Account;
import utescore.service.AccountService;
import utescore.service.OtpService;

import jakarta.servlet.http.HttpSession;
import java.util.HashMap;
import java.util.Map;

@Controller
@RequestMapping("/auth")
public class AuthController {

    private final AccountService accountService;
    private final OtpService otpService;

    public AuthController(AccountService accountService, OtpService otpService) {
        this.accountService = accountService;
        this.otpService = otpService;
    }

    @GetMapping("/login")
    public String loginPage(Model model,
                            @RequestParam(value = "error", required = false) String error,
                            @RequestParam(value = "logout", required = false) String logout) {
        if (error != null) {
            if ("access_denied".equals(error)) {
                model.addAttribute("error", "Please login to access this page.");
            } else {
                model.addAttribute("error", "Invalid username or password.");
            }
        }

        if (logout != null) {
            model.addAttribute("message", "You have been logged out successfully.");
        }

        return "auth/login";
    }

    @GetMapping("/register")
    public String registerPage(Model model) {
        model.addAttribute("registerRequest", new RegisterRequest());
        return "auth/register";
    }

    @PostMapping("/register")
    public String register(@Valid @ModelAttribute RegisterRequest registerRequest,
                          BindingResult result,
                          RedirectAttributes redirectAttributes,
                          Model model,
                          HttpSession session) {
        if (result.hasErrors()) {
            return "auth/register";
        }

        try {
            // Validate that username and email are not already taken
            if (accountService.existsByUsername(registerRequest.getUsername())) {
                model.addAttribute("error", "Username is already taken");
                return "auth/register";
            }

            if (accountService.existsByEmail(registerRequest.getEmail())) {
                model.addAttribute("error", "Email is already registered");
                return "auth/register";
            }

            // Validate password confirmation
            if (!registerRequest.getPassword().equals(registerRequest.getConfirmPassword())) {
                model.addAttribute("error", "Passwords do not match");
                return "auth/register";
            }

            // Store registration data in session (don't create account yet)
            session.setAttribute("pendingRegistration", registerRequest);

            // Send OTP
            otpService.generateAndSendOtp(registerRequest.getEmail());

            redirectAttributes.addFlashAttribute("message", "Please verify your email with the OTP sent to " + registerRequest.getEmail());
            return "redirect:/auth/verify-otp?email=" + registerRequest.getEmail();

        } catch (Exception e) {
            model.addAttribute("error", "Registration failed: " + e.getMessage());
            return "auth/register";
        }
    }

    @GetMapping("/verify-otp")
    public String otpVerificationPage(@RequestParam(value = "email", required = false) String email,
                                      Model model, HttpSession session) {
        RegisterRequest pending = (RegisterRequest) session.getAttribute("pendingRegistration");
        if (pending == null && email == null) {
            return "redirect:/auth/register";
        }

        OtpVerificationRequest otpRequest = new OtpVerificationRequest();
        if (pending != null) otpRequest.setEmail(pending.getEmail());
        else otpRequest.setEmail(email);

        model.addAttribute("otpRequest", otpRequest);
        return "auth/verify-otp";
    }

    @PostMapping("/verify-otp")
    public String verifyOtp(@Valid @ModelAttribute("otpRequest") OtpVerificationRequest otpRequest,
                            BindingResult result,
                            RedirectAttributes redirectAttributes,
                            Model model,
                            HttpSession session) {

        RegisterRequest pending = (RegisterRequest) session.getAttribute("pendingRegistration");
        if (pending == null) {
            redirectAttributes.addFlashAttribute("error", "No pending registration found. Please register again.");
            return "redirect:/auth/register";
        }

        String email = pending.getEmail();

        if (result.hasErrors()) return "auth/verify-otp";

        try {
            boolean isValid = otpService.validateOtp(email, otpRequest.getOtp());

            if (isValid) {
                Account account = accountService.createAccount(pending);
                session.removeAttribute("pendingRegistration");
                redirectAttributes.addFlashAttribute("message", "Registration completed successfully! You can now login.");
                return "redirect:/auth/login";
            } else {
                model.addAttribute("error", "Invalid or expired OTP");
                return "auth/verify-otp";
            }

        } catch (Exception e) {
            model.addAttribute("error", e.getMessage());
            return "auth/verify-otp";
        }
    }

    @PostMapping("/resend-otp")
    @ResponseBody
    public ResponseEntity<Map<String, String>> resendOtp(HttpSession session) {
        Map<String, String> response = new HashMap<>();
        try {
            RegisterRequest pending = (RegisterRequest) session.getAttribute("pendingRegistration");
            if (pending == null) {
                response.put("error", "No pending registration found");
                return ResponseEntity.badRequest().body(response);
            }

            otpService.generateAndSendOtp(pending.getEmail());
            response.put("message", "OTP sent successfully!");
            return ResponseEntity.ok(response);

        } catch (Exception e) {
            response.put("error", "Failed to send OTP: " + e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }
}