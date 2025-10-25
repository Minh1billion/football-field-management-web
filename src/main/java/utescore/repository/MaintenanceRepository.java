package utescore.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import utescore.entity.Maintenance;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

public interface MaintenanceRepository extends JpaRepository<Maintenance, Long> {

    @Query("""
        select m from Maintenance m
        where m.field.id = :fieldId
          and m.status in (utescore.entity.Maintenance.MaintenanceStatus.SCHEDULED, 
                           utescore.entity.Maintenance.MaintenanceStatus.IN_PROGRESS)
        """)
    List<Maintenance> findActiveMaintenancesByField(@Param("fieldId") Long fieldId);

    default List<Maintenance> findPlannedInRange(Long fieldId, LocalDateTime start, LocalDateTime end) {
        return findActiveMaintenancesByField(fieldId).stream()
                .filter(m -> {
                    // Tính thời gian kết thúc bảo trì
                    int duration = (m.getEstimatedDurationHours() != null) ? m.getEstimatedDurationHours() : 2;
                    LocalDateTime maintenanceEnd = m.getScheduledDate().plusHours(duration);

                    // Check overlap
                    return m.getScheduledDate().isBefore(end) && maintenanceEnd.isAfter(start);
                })
                .collect(Collectors.toList());
    }

    long countByField_ManagerIdAndScheduledDateBetween(Long managerId, LocalDateTime start, LocalDateTime end);

    @Query("""
    select count(distinct m.field.id) from Maintenance m
    where m.field.managerId = :managerId
      and m.status = utescore.entity.Maintenance.MaintenanceStatus.IN_PROGRESS
    """)
    long countActiveMaintenanceFieldsByManagerUsername(@Param("managerId") Long managerId);

    long countByField_IdAndStatusIn(Long fieldId, java.util.Collection<Maintenance.MaintenanceStatus> statuses);
}