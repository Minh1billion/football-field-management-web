package utescore.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import utescore.entity.Booking;
import utescore.entity.Payment;
import utescore.repository.BookingRepository;
import utescore.repository.PaymentRepository;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ReportService {

    private final PaymentRepository paymentRepository;
    private final BookingRepository bookingRepository;

    public Map<Integer, BigDecimal> revenueByMonth(int year) {
        LocalDateTime start = LocalDate.of(year, 1, 1).atStartOfDay();
        LocalDateTime end = LocalDate.of(year, 12, 31).atTime(23, 59, 59);
        List<Payment> completed = paymentRepository.findByStatusAndPaidAtBetween(
                Payment.PaymentStatus.COMPLETED, start, end);

        Map<Integer, BigDecimal> map = new LinkedHashMap<>();
        for (int m = 1; m <= 12; m++) map.put(m, BigDecimal.ZERO);

        for (Payment p : completed) {
            if (p.getPaidAt() != null) {
                int m = p.getPaidAt().getMonthValue();
                map.put(m, map.get(m).add(p.getAmount()));
            }
        }
        return map;
    }

    public BigDecimal revenueInRange(LocalDateTime start, LocalDateTime end) {
        List<Payment> completed = paymentRepository.findByStatusAndPaidAtBetween(
                Payment.PaymentStatus.COMPLETED, start, end);
        return completed.stream()
                .map(Payment::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    public Map<String, BigDecimal> revenueByField(LocalDateTime start, LocalDateTime end) {
        List<Payment> completed = paymentRepository.findByStatusAndPaidAtBetween(
                Payment.PaymentStatus.COMPLETED, start, end);

        return completed.stream()
                .filter(p -> p.getBooking() != null && p.getBooking().getField() != null)
                .collect(Collectors.groupingBy(
                        p -> p.getBooking().getField().getName(),
                        LinkedHashMap::new,
                        Collectors.reducing(BigDecimal.ZERO, Payment::getAmount, BigDecimal::add)
                ));
    }

    public Map<Booking.BookingStatus, Long> bookingStatusStats() {
        Map<Booking.BookingStatus, Long> res = new EnumMap<>(Booking.BookingStatus.class);
        for (Booking.BookingStatus s : Booking.BookingStatus.values()) {
            res.put(s, (long) bookingRepository.findByStatus(s).size());
        }
        return res;
    }
}