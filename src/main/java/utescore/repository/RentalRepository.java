package utescore.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import utescore.entity.BookingSportWear;

public interface RentalRepository extends JpaRepository<BookingSportWear, Long> {
    @Query("SELECT COUNT(r) FROM BookingSportWear r WHERE r.booking.customer.account.username = :username AND r.status = 'RETURNED'")
    long countActiveRentals(String username);
}
