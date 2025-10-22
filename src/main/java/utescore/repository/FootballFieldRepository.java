package utescore.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import utescore.entity.FootballField;

import java.util.List;

public interface FootballFieldRepository extends JpaRepository<FootballField, Long> {
    List<FootballField> findByLocation_Id(Long locationId);
    List<FootballField> findByIsActiveTrue();
    long countByManagerId(Long managerId);
}