package utescore.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import utescore.entity.Loyalty;

public interface LoyaltyRepository extends JpaRepository<Loyalty, Long> {
    Loyalty findByCustomer_Account_Username(String username);
}
