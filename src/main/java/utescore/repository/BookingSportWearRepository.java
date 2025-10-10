package utescore.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import utescore.entity.BookingSportWear;

public interface BookingSportWearRepository extends JpaRepository<BookingSportWear, Long> {
}