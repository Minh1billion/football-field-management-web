package utescore.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import utescore.entity.Log;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface LogRepository extends JpaRepository<Log, Long> {
	List<Log> findAllByType(String type);

	@Query("SELECT l FROM Log l WHERE l.type = 'MAINTENANCE' AND :now BETWEEN l.createdAt AND l.endDateTime")
	Optional<Log> findActiveMaintenance(@Param("now") LocalDateTime now);
}
