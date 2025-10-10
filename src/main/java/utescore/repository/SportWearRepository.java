package utescore.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import utescore.entity.SportWear;

public interface SportWearRepository extends JpaRepository<SportWear, Long> {
}