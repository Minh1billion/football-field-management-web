package utescore.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import utescore.entity.Location;

public interface LocationRepository extends JpaRepository<Location, Long> {
}