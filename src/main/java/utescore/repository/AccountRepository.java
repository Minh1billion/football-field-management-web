package utescore.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import utescore.entity.Account;

import java.util.List;
import java.util.Optional;

@Repository
public interface AccountRepository extends JpaRepository<Account, Long> {
    Optional<Account> findByUsername(String username);
    Optional<Account> findByEmail(String email);
    Optional<Account> findByUsernameOrEmail(String username, String email);
    boolean existsByUsername(String username);
    boolean existsByEmail(String email);

    long countByRole(Account.Role role);
    long countByIsActive(boolean isActive);
    long countByIsActiveTrue();
    long countByIsActiveFalse();

    @Query("SELECT a FROM Account a WHERE " +
           "(:email IS NULL OR a.email LIKE %:email%) AND " +
           "(:role IS NULL OR a.role = :role)")
    Page<Account> findByEmailContainingAndRole(@Param("email") String email,
                                               @Param("role") Account.Role role,
                                               Pageable pageable);

    // New: find users by role
    List<Account> findByRole(Account.Role role);
    @Query("SELECT a.role FROM Account a WHERE a.username = :username")
    String findRoleByUsername(String username);
}