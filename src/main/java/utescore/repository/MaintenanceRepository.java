package utescore.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import utescore.entity.Maintenance;

import java.time.LocalDateTime;
import java.util.List;

public interface MaintenanceRepository extends JpaRepository<Maintenance, Long> {

    @Query("""
        select m from Maintenance m
        where m.field.id = :fieldId
          and m.status in (utescore.entity.Maintenance.MaintenanceStatus.SCHEDULED, utescore.entity.Maintenance.MaintenanceStatus.IN_PROGRESS)
          and m.scheduledDate between :start and :end
        """)
    List<Maintenance> findPlannedInRange(@Param("fieldId") Long fieldId,
                                         @Param("start") LocalDateTime start,
                                         @Param("end") LocalDateTime end);
}