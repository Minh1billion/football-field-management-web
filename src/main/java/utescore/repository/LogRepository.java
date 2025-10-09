package utescore.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import utescore.entity.Log;

public interface LogRepository extends JpaRepository<Log, Long> {
}
