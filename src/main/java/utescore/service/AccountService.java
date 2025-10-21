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
import org.springframework.security.core.userdetails.UserDetails;

import java.math.BigDecimal;
import java.util.Optional;

@Service
@Transactional
public class AccountService {

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

    public Account createAccount(RegisterRequest registerRequest) {
        // Validate passwords match
        if (!registerRequest.isPasswordMatching()) {
            throw new IllegalArgumentException("Passwords do not match");
        }

        // Check if username already exists
        if (accountRepository.existsByUsername(registerRequest.getUsername())) {
            throw new IllegalArgumentException("Username already exists");
        }

        // Check if email already exists
        if (accountRepository.existsByEmail(registerRequest.getEmail())) {
            throw new IllegalArgumentException("Email already exists");
        }

        // Create account
        Account account = new Account();
        account.setUsername(registerRequest.getUsername());
        account.setEmail(registerRequest.getEmail());
        account.setPassword(passwordEncoder.encode(registerRequest.getPassword()));
        account.setRole(Account.Role.USER);
        account.setIsActive(true);

        Account savedAccount = accountRepository.save(account);

        // Create customer profile
        Customer customer = new Customer();
        customer.setFirstName(registerRequest.getFirstName());
        customer.setLastName(registerRequest.getLastName());
        customer.setPhoneNumber(registerRequest.getPhoneNumber());
        customer.setDateOfBirth(registerRequest.getDateOfBirth());
        customer.setGender(registerRequest.getGender());
        customer.setAddress(registerRequest.getAddress());
        customer.setEmergencyContact(registerRequest.getEmergencyContact());
        customer.setEmergencyPhone(registerRequest.getEmergencyPhone());
        customer.setAccount(savedAccount);
        customer.setCreatedAt(java.time.LocalDateTime.now());

        Customer savedCustomer = customerRepository.save(customer);

        // Create loyalty program
        Loyalty loyalty = new Loyalty();
        loyalty.setCustomer(savedCustomer);
        loyalty.setPoints(0);
        loyalty.setTier(Loyalty.MembershipTier.BRONZE);
        loyalty.setTotalSpent(BigDecimal.ZERO);
        loyalty.setTotalBookings(0);
        savedCustomer.setLoyalty(loyalty);
        
        logService.logAction("New user registered: " + savedAccount.getUsername(), savedAccount);
        
        
        return savedAccount;
    }

    public Account createAccountByRole(RegisterRequest registerRequest, String role) {
        if (!registerRequest.isPasswordMatching()) {
            throw new IllegalArgumentException("Passwords do not match");
        }

        if (accountRepository.existsByUsername(registerRequest.getUsername())) {
            throw new IllegalArgumentException("Username already exists");
        }

        if (accountRepository.existsByEmail(registerRequest.getEmail())) {
            throw new IllegalArgumentException("Email already exists");
        }

        // Create account
        Account account = new Account();
        account.setUsername(registerRequest.getUsername());
        account.setEmail(registerRequest.getEmail());
        account.setPassword(passwordEncoder.encode(registerRequest.getPassword()));
        account.setRole(Account.Role.valueOf(role));
        account.setIsActive(true);

        Account savedAccount = accountRepository.save(account);

        String currentUser = null;
        if (SecurityContextHolder.getContext().getAuthentication() != null) {
            currentUser = SecurityContextHolder.getContext().getAuthentication().getName();
        }

        logService.logAction(
                "New user registered: " + savedAccount.getUsername() + " with " + role,
                currentUser != null
                        ? accountRepository.findByUsername(currentUser).orElse(null)
                        : null
        );

        return savedAccount;
    }

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

    public Account updateAccount(Account account) {
        return accountRepository.save(account);
    }

    public void deleteAccount(Long id) {
        accountRepository.deleteById(id);
    }

    public Account changePassword(String username, String newPassword) {
        Account account = accountRepository.findByUsername(username)
                .orElseThrow(() -> new IllegalArgumentException("Account not found"));
        
        account.setPassword(passwordEncoder.encode(newPassword));
        return accountRepository.save(account);
    }

    public Account activateAccount(String username) {
        Account account = accountRepository.findByUsername(username)
                .orElseThrow(() -> new IllegalArgumentException("Account not found"));
        
        account.setIsActive(true);
        return accountRepository.save(account);
    }

    public Account deactivateAccount(String username) {
        Account account = accountRepository.findByUsername(username)
                .orElseThrow(() -> new IllegalArgumentException("Account not found"));
        
        account.setIsActive(false);
        return accountRepository.save(account);
    }

	public Page<Account> findAll(Pageable pageable) {
		return accountRepository.findAll(pageable);
	}

	public Account findById(Long id) {
		return accountRepository.findById(id).orElse(null);
	}

	public void deleteById(Long id) {
		accountRepository.deleteById(id);	
	}

	public Page<Account> findByEmailAndRole(String email, String role, Pageable pageable) {
		Account.Role roleEnum = null;
		if (role != null && !role.isEmpty()) {
			try {
				roleEnum = Account.Role.valueOf(role.toUpperCase());
			} catch (IllegalArgumentException e) {
				throw new IllegalArgumentException("Invalid role: " + role);
			}
		}
		return accountRepository.findByEmailContainingAndRole(email, roleEnum, pageable);
	}

	public long countAllAccounts() {
		return accountRepository.count();
	}

	public long countByRole(String role) {
		try {
			return accountRepository.countByRole(Account.Role.valueOf(role));
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
}