package utescore.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import utescore.entity.Review;

@Repository
public interface ReviewRepository extends JpaRepository<Review, Long> {
    long countByCustomer_Account_Username(String username);
}
