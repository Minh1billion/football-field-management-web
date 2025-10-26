package utescore.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import utescore.dto.RegisterRequest;
import utescore.entity.Account;
import utescore.entity.Customer;
import utescore.entity.Loyalty;
import utescore.repository.AccountRepository;
import utescore.repository.CustomerRepository;

import org.springframework.security.core.context.SecurityContextHolder;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.regex.Pattern;

@Service
public class AccountService {

    private static final Pattern EMAIL_PATTERN = Pattern.compile(
            "^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$"
    );

    private static final Pattern PHONE_PATTERN = Pattern.compile(
            "^[0-9]{10,15}$"
    );

    private static final Set<String> ALLOWED_ROLES = Set.of("USER", "ADMIN", "MANAGER");

    private final AccountRepository accountRepository;
    private final CustomerRepository customerRepository;
    private final PasswordEncoder passwordEncoder;

    @Autowired
    private LogService logService;

    public AccountService(AccountRepository accountRepository,
                          CustomerRepository customerRepository,
                          PasswordEncoder passwordEncoder) {
        this.accountRepository = accountRepository;
        this.customerRepository = customerRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Transactional
    public Account createAccount(RegisterRequest registerRequest) {
        validateRegisterRequest(registerRequest);

        // Check if username already exists
        if (accountRepository.existsByUsername(registerRequest.getUsername())) {
            throw new IllegalArgumentException("Username already exists");
        }

        // Check if email already exists
        if (accountRepository.existsByEmail(registerRequest.getEmail())) {
            throw new IllegalArgumentException("Email already exists");
        }

        // Create account
        Account account = buildAccount(registerRequest, Account.Role.USER);
        Account savedAccount = accountRepository.save(account);

        // Create customer profile with loyalty
        Customer customer = buildCustomer(registerRequest, savedAccount);
        Loyalty loyalty = buildLoyalty();

        // Set bidirectional relationship (IMPORTANT: set both sides)
        loyalty.setCustomer(customer);
        customer.setLoyalty(loyalty);

        // Save customer (cascade will save loyalty)
        customerRepository.save(customer);

        logService.logAction(
                "New user registered: " + savedAccount.getUsername(),
                savedAccount,
                "SYSTEM"
        );

        return savedAccount;
    }

    @Transactional
    public Account createAccountByRole(RegisterRequest registerRequest, String role) {
        validateRegisterRequest(registerRequest);
        validateRole(role);

        // Check username/email existence
        if (accountRepository.existsByUsername(registerRequest.getUsername())) {
            throw new IllegalArgumentException("Username already exists");
        }
        if (accountRepository.existsByEmail(registerRequest.getEmail())) {
            throw new IllegalArgumentException("Email already exists");
        }

        // Create new account
        Account.Role roleEnum = Account.Role.valueOf(role.toUpperCase());
        Account account = buildAccount(registerRequest, roleEnum);
        Account savedAccount = accountRepository.save(account);

        // If USER role, create Customer + Loyalty
        if (Account.Role.USER.equals(roleEnum)) {
            Customer customer = buildCustomer(registerRequest, savedAccount);
            Loyalty loyalty = buildLoyalty();

            // Set bidirectional relationship
            loyalty.setCustomer(customer);
            customer.setLoyalty(loyalty);

            customerRepository.save(customer);
        }

        // Log action
        String currentUsername = getCurrentUsername();
        Account currentAccount = currentUsername != null
                ? accountRepository.findByUsername(currentUsername).orElse(null)
                : null;

        logService.logAction(
                "New " + role + " account created: " + savedAccount.getUsername(),
                currentAccount,
                "SYSTEM"
        );

        return savedAccount;
    }

    // Validation methods
    private void validateRegisterRequest(RegisterRequest request) {
        if (!request.isPasswordMatching()) {
            throw new IllegalArgumentException("Passwords do not match");
        }

        if (request.getPassword() == null || request.getPassword().length() < 8) {
            throw new IllegalArgumentException("Password must be at least 8 characters");
        }

        if (!EMAIL_PATTERN.matcher(request.getEmail()).matches()) {
            throw new IllegalArgumentException("Invalid email format");
        }

        if (request.getPhoneNumber() != null &&
                !PHONE_PATTERN.matcher(request.getPhoneNumber()).matches()) {
            throw new IllegalArgumentException("Invalid phone number format");
        }
    }

    private void validateRole(String role) {
        if (role == null || !ALLOWED_ROLES.contains(role.toUpperCase())) {
            throw new IllegalArgumentException("Invalid role: " + role);
        }
    }

    // Builder methods
    private Account buildAccount(RegisterRequest request, Account.Role role) {
        Account account = new Account();
        account.setUsername(request.getUsername());
        account.setEmail(request.getEmail());
        account.setPassword(passwordEncoder.encode(request.getPassword()));
        account.setRole(role);
        account.setIsActive(true);
        return account;
    }

    private Customer buildCustomer(RegisterRequest request, Account account) {
        Customer customer = new Customer();
        customer.setFirstName(request.getFirstName());
        customer.setLastName(request.getLastName());
        customer.setPhoneNumber(request.getPhoneNumber());
        customer.setDateOfBirth(request.getDateOfBirth());
        customer.setGender(request.getGender());
        customer.setAddress(request.getAddress());
        customer.setEmergencyContact(request.getEmergencyContact());
        customer.setEmergencyPhone(request.getEmergencyPhone());
        customer.setAccount(account);
        customer.setCreatedAt(LocalDateTime.now());
        return customer;
    }

    private Loyalty buildLoyalty() {
        Loyalty loyalty = new Loyalty();
        loyalty.setPoints(0);
        loyalty.setTier(Loyalty.MembershipTier.BRONZE);
        loyalty.setTotalSpent(BigDecimal.ZERO);
        loyalty.setTotalBookings(0);
        return loyalty;
    }

    private String getCurrentUsername() {
        try {
            return SecurityContextHolder.getContext()
                    .getAuthentication()
                    .getName();
        } catch (Exception e) {
            return null;
        }
    }

    // Query methods
    public Optional<Account> findByUsername(String username) {
        return accountRepository.findByUsername(username);
    }

    public Optional<Account> findByEmail(String email) {
        return accountRepository.findByEmail(email);
    }

    public Optional<Account> findByUsernameOrEmail(String username, String email) {
        return accountRepository.findByUsernameOrEmail(username, email);
    }

    public boolean existsByUsername(String username) {
        return accountRepository.existsByUsername(username);
    }

    public boolean existsByEmail(String email) {
        return accountRepository.existsByEmail(email);
    }

    @Transactional
    public Account updateAccount(Account account) {
        return accountRepository.save(account);
    }

    @Transactional
    public void deleteAccount(Long id) {
        if (!accountRepository.existsById(id)) {
            throw new IllegalArgumentException("Account not found with id: " + id);
        }
        accountRepository.deleteById(id);
    }

    @Transactional
    public Account changePassword(String username, String newPassword) {
        if (newPassword == null || newPassword.length() < 8) {
            throw new IllegalArgumentException("Password must be at least 8 characters");
        }

        Account account = accountRepository.findByUsername(username)
                .orElseThrow(() -> new IllegalArgumentException("Account not found"));

        account.setPassword(passwordEncoder.encode(newPassword));
        return accountRepository.save(account);
    }

    @Transactional
    public Account activateAccount(String username) {
        Account account = accountRepository.findByUsername(username)
                .orElseThrow(() -> new IllegalArgumentException("Account not found"));

        account.setIsActive(true);
        return accountRepository.save(account);
    }

    @Transactional
    public Account deactivateAccount(String username) {
        Account account = accountRepository.findByUsername(username)
                .orElseThrow(() -> new IllegalArgumentException("Account not found"));

        account.setIsActive(false);
        return accountRepository.save(account);
    }

    public Page<Account> findAll(Pageable pageable) {
        return accountRepository.findAll(pageable);
    }

    public List<Account> getAllUsers() {
        return accountRepository.findByRole(Account.Role.USER);
    }

    public Account findById(Long id) {
        return accountRepository.findById(id).orElse(null);
    }

    @Transactional
    public void deleteById(Long id) {
        if (!accountRepository.existsById(id)) {
            throw new IllegalArgumentException("Account not found with id: " + id);
        }
        accountRepository.deleteById(id);
    }

    public Page<Account> findByEmailAndRole(String email, String role, Pageable pageable) {
        Account.Role roleEnum = null;
        if (role != null && !role.isEmpty()) {
            validateRole(role);
            roleEnum = Account.Role.valueOf(role.toUpperCase());
        }
        return accountRepository.findByEmailContainingAndRole(email, roleEnum, pageable);
    }

    public long countAllAccounts() {
        return accountRepository.count();
    }

    public long countByRole(String role) {
        try {
            validateRole(role);
            return accountRepository.countByRole(Account.Role.valueOf(role.toUpperCase()));
        } catch (IllegalArgumentException e) {
            return 0;
        }
    }

    public long countActiveAccounts() {
        return accountRepository.countByIsActiveTrue();
    }

    public long countInactiveAccounts() {
        return accountRepository.countByIsActiveFalse();
    }

    public List<Account> findByRole(String role) {
        try {
            validateRole(role);
            return accountRepository.findByRole(Account.Role.valueOf(role.toUpperCase()));
        } catch (IllegalArgumentException e) {
            return List.of();
        }
    }

    public boolean verifyPassword(String username, String rawPassword) {
        Account account = accountRepository.findByUsername(username)
                .orElseThrow(() -> new IllegalArgumentException("Account not found"));

        return passwordEncoder.matches(rawPassword, account.getPassword());
    }

    @Transactional
    public Account changePasswordWithVerification(String username, String currentPassword, String newPassword) {
        // Verify current password
        if (!verifyPassword(username, currentPassword)) {
            throw new IllegalArgumentException("Current password is incorrect");
        }

        // Validate new password
        if (newPassword == null || newPassword.length() < 8) {
            throw new IllegalArgumentException("New password must be at least 8 characters");
        }

        // Update password
        Account account = accountRepository.findByUsername(username)
                .orElseThrow(() -> new IllegalArgumentException("Account not found"));

        account.setPassword(passwordEncoder.encode(newPassword));
        Account savedAccount = accountRepository.save(account);

        // Log action
        logService.logAction("Password changed", account, "USER");

        return savedAccount;
    }
}