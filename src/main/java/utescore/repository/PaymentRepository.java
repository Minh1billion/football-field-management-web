package utescore.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import utescore.entity.Payment;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface PaymentRepository extends JpaRepository<Payment, Long> {

    Optional<Payment> findByPaymentCode(String paymentCode);

    Optional<Payment> findByTransactionId(String transactionId);

    Optional<Payment> findByOrderId(Long orderId);

    Optional<Payment> findByBookingId(Long bookingId);

    @Query("SELECT p FROM Payment p WHERE p.order.id = :orderId")
    Optional<Payment> findPaymentByOrderId(@Param("orderId") Long orderId);

    @Query("SELECT p FROM Payment p WHERE p.booking.id = :bookingId")
    Optional<Payment> findPaymentByBookingId(@Param("bookingId") Long bookingId);

    List<Payment> findByStatus(Payment.PaymentStatus status);

    List<Payment> findByStatusAndPaidAtBetween(Payment.PaymentStatus status,
                                               LocalDateTime start, LocalDateTime end);

    @Query("SELECT p FROM Payment p " +
            "LEFT JOIN p.booking b " +
            "LEFT JOIN p.order o " +
            "LEFT JOIN p.rentalOrder r " +
            "WHERE (b.id IS NOT NULL AND b.customer.id = :customerId) " +
            "OR (o.id IS NOT NULL AND o.customer.id = :customerId) " +
            "OR (r.id IS NOT NULL AND r.customer.id = :customerId)")
    List<Payment> findByCustomerId(@Param("customerId") Long customerId);

    @Query("SELECT p FROM Payment p " +
            "LEFT JOIN p.booking b " +
            "LEFT JOIN p.order o " +
            "LEFT JOIN p.rentalOrder r " +
            "WHERE p.id = :paymentId " +
            "AND ((b.id IS NOT NULL AND b.customer.id = :customerId) " +
            "OR (o.id IS NOT NULL AND o.customer.id = :customerId) " +
            "OR (r.id IS NOT NULL AND r.customer.id = :customerId))")
    Optional<Payment> findByIdAndCustomerId(@Param("paymentId") Long paymentId,
                                            @Param("customerId") Long customerId);
}