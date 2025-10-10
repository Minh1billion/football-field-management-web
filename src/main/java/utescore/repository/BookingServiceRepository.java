package utescore.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import utescore.entity.BookingService;

public interface BookingServiceRepository extends JpaRepository<BookingService, Long> {
}