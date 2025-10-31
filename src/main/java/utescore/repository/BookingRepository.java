package utescore.repository;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import utescore.entity.Booking;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface BookingRepository extends JpaRepository<Booking, Long> {

    @EntityGraph(attributePaths = {
            "customer",
            "field",
            "payment",
            "bookingSportWears",
            "bookingSportWears.sportWear",
            "bookingServices",
            "bookingServices.service"
    })
    Optional<Booking> findById(Long id);

    @Query("""
        SELECT CASE WHEN COUNT(b) > 0 THEN true ELSE false END
        FROM Booking b
        WHERE b.field.id = :fieldId
          AND b.status IN ('PENDING', 'CONFIRMED', 'COMPLETED')
          AND (
              (b.startTime < :end AND b.endTime > :start)
          )
    """)
    boolean existsOverlap(@Param("fieldId") Long fieldId,
                          @Param("start") LocalDateTime start,
                          @Param("end") LocalDateTime end);

    List<Booking> findByStatus(Booking.BookingStatus status);

    @Query("SELECT COUNT(b) FROM Booking b WHERE b.customer.account.username = :username AND b.startTime > :now")
    long countUpcomingBookings(String username, LocalDateTime now);

    @Query("SELECT SUM(b.totalAmount) FROM Booking b WHERE b.customer.account.username = :username AND FUNCTION('MONTH', b.startTime) = :month AND FUNCTION('YEAR', b.startTime) = :year")
    Long calculateMonthlySpending(String username, int month, int year);

    List<Booking> findByCustomer_Account_UsernameOrderByCreatedAt(String username);

    long countByField_ManagerIdAndStartTimeBetween(Long managerId, LocalDateTime start, LocalDateTime end);

    @Query("""
    SELECT COALESCE(SUM(b.totalAmount), 0)
    FROM Booking b
    WHERE b.field.managerId = :managerId
      AND b.startTime >= :start
      AND b.startTime < :end
    """)
    BigDecimal sumTotalAmountByManagerAndDateRange(
            @Param("managerId") Long managerId,
            @Param("start") LocalDateTime start,
            @Param("end") LocalDateTime end
    );

    @Query("""
        SELECT COUNT(DISTINCT b.customer.id)
        FROM Booking b
        WHERE b.field.managerId = :managerId
          AND b.startTime >= :start
          AND b.startTime < :end
    """)
    long countActiveCustomersByManagerAndDateRange(
            @Param("managerId") Long managerId,
            @Param("start") LocalDateTime start,
            @Param("end") LocalDateTime end
    );

    long countByField_ManagerIdAndStatus(Long managerId, Booking.BookingStatus status);

    @Query("""
    SELECT COUNT(DISTINCT b.field.id)
    FROM Booking b
    WHERE b.field.managerId = :managerId
      AND b.status IN (utescore.entity.Booking.BookingStatus.CONFIRMED, 
                       utescore.entity.Booking.BookingStatus.PENDING)
      AND b.startTime <= :now
      AND b.endTime >= :now
    """)
    long countActiveFieldsByManagerId(@Param("managerId") Long managerId,
                                      @Param("now") LocalDateTime now);

    @Query("""
    SELECT COALESCE(SUM(TIMESTAMPDIFF(HOUR, b.startTime, b.endTime)), 0)
    FROM Booking b
    WHERE b.field.managerId = :managerId
      AND b.status IN (utescore.entity.Booking.BookingStatus.CONFIRMED, 
                       utescore.entity.Booking.BookingStatus.PENDING)
      AND b.startTime >= :start
      AND b.startTime < :end
    """)
    long countBookedSlotsByManagerIdAndDateRange(@Param("managerId") Long managerId,
                                                 @Param("start") LocalDateTime start,
                                                 @Param("end") LocalDateTime end);
    @Query("""
        SELECT b FROM Booking b
        WHERE b.customer.account.username = :username
          AND b.status IN ('PENDING', 'CONFIRMED')
          AND b.endTime > :now
        ORDER BY b.startTime ASC
    """)
    List<Booking> findActiveBookingsByUsername(
            @Param("username") String username,
            @Param("now") LocalDateTime now
    );

    @Query("""
        SELECT b FROM Booking b
        WHERE b.customer.account.username = :username
          AND b.status IN ('PENDING', 'CONFIRMED')
          AND b.startTime >= :now
          AND b.startTime <= :endDate
        ORDER BY b.startTime ASC
    """)
    List<Booking> findUpcomingBookingsByUsername(
            @Param("username") String username,
            @Param("now") LocalDateTime now,
            @Param("endDate") LocalDateTime endDate
    );
}