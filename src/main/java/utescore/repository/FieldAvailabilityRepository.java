package utescore.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import utescore.entity.FieldAvailability;

import java.time.LocalDateTime;
import java.util.List;

public interface FieldAvailabilityRepository extends JpaRepository<FieldAvailability, Long> {

    // Các khoảng thời gian bị đánh dấu isAvailable=false và overlap với [start, end]
    @Query("""
        select fa from FieldAvailability fa
        where fa.field.id = :fieldId
          and fa.isAvailable = false
          and (fa.startTime < :end and fa.endTime > :start)
        """)
    List<FieldAvailability> findBlockingWindows(@Param("fieldId") Long fieldId,
                                                @Param("start") LocalDateTime start,
                                                @Param("end") LocalDateTime end);
}