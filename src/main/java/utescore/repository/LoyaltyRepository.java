package utescore.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import utescore.entity.Loyalty;

import java.util.Optional;

public interface LoyaltyRepository extends JpaRepository<Loyalty, Long> {
    Loyalty findByCustomer_Account_Username(String username);
    Optional<Loyalty> findByCustomer_Id(Long customerId);
}
