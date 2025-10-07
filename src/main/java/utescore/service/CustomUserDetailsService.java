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
        Account account = accountRepository.findByUsernameOrEmail(username, username)
                .orElseThrow(() -> new UsernameNotFoundException("User not found: " + username));

        if (!account.getIsActive()) {
            throw new UsernameNotFoundException("Account is disabled: " + username);
        }

        return User.builder()
                .username(account.getUsername())
                .password(account.getPassword())
                .authorities(getAuthorities(account))
                .accountExpired(false)
                .accountLocked(!account.getIsActive())
                .credentialsExpired(false)
                .disabled(!account.getIsActive())
                .build();
    }

    private Collection<? extends GrantedAuthority> getAuthorities(Account account) {
        return Collections.singletonList(new SimpleGrantedAuthority("ROLE_" + account.getRole().name()));
    }

    public Account findAccountByUsername(String username) {
        return accountRepository.findByUsernameOrEmail(username, username)
                .orElse(null);
    }
}