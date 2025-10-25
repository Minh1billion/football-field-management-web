package utescore.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import utescore.entity.Customer;
import utescore.entity.FootballField;
import utescore.entity.Review;

import java.util.List;

@Repository
public interface ReviewRepository extends JpaRepository<Review, Long> {

    long countByCustomer_Account_Username(String username);

    List<Review> findByCustomer_Account_UsernameOrderByCreatedAtDesc(String username);

    List<Review> findByField_IdOrderByCreatedAtDesc(Long fieldId);

    boolean existsByCustomerAndField(Customer customer, FootballField field);
}