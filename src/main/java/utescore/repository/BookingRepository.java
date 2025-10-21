package utescore.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import utescore.entity.Booking;

import java.time.LocalDateTime;
import java.util.List;

public interface BookingRepository extends JpaRepository<Booking, Long> {

    // Kiểm tra trùng slot (overlap) với các booking chưa hủy
    @Query("""
        select count(b) > 0 from Booking b
        where b.field.id = :fieldId
          and b.status in (utescore.entity.Booking.BookingStatus.PENDING, utescore.entity.Booking.BookingStatus.CONFIRMED)
          and (b.startTime < :end and b.endTime > :start)
        """)
    boolean existsOverlap(@Param("fieldId") Long fieldId,
                          @Param("start") LocalDateTime start,
                          @Param("end") LocalDateTime end);

    List<Booking> findByStatus(Booking.BookingStatus status);
}

