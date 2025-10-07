package utescore.service;

import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import utescore.entity.Account;
import utescore.repository.AccountRepository;

import java.util.Collection;
import java.util.Collections;

@Service
public class CustomUserDetailsService implements UserDetailsService {

    private final AccountRepository accountRepository;

    public CustomUserDetailsService(AccountRepository accountRepository) {
        this.accountRepository = accountRepository;
    }

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        System.out.println("Loading user: " + username);

        Account account = accountRepository.findByUsernameOrEmail(username, username)
                .orElseThrow(() -> new UsernameNotFoundException("User not found: " + username));

        System.out.println("Found account: " + account.getUsername());
        System.out.println("Password from DB: " + account.getPassword());
        System.out.println("Role: " + account.getRole().name());
        System.out.println("Is Active: " + account.getIsActive());

        if (!account.getIsActive()) {
            throw new UsernameNotFoundException("Account is disabled: " + username);
        }

        UserDetails userDetails = User.builder()
                .username(account.getUsername())
                .password(account.getPassword())
                .authorities(getAuthorities(account))
                .accountExpired(false)
                .accountLocked(!account.getIsActive())
                .credentialsExpired(false)
                .disabled(!account.getIsActive())
                .build();

        System.out.println("UserDetails created with authorities: " + userDetails.getAuthorities());

        return userDetails;
    }

    private Collection<? extends GrantedAuthority> getAuthorities(Account account) {
        return Collections.singletonList(new SimpleGrantedAuthority("ROLE_" + account.getRole().name()));
    }

    public Account findAccountByUsername(String username) {
        return accountRepository.findByUsernameOrEmail(username, username)
                .orElse(null);
    }
}