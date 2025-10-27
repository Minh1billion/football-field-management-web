package utescore.service;

import lombok.RequiredArgsConstructor;
import org.springframework.transaction.annotation.Transactional;
import utescore.dto.BookingRequestDTO;
import utescore.entity.*;
import utescore.entity.BookingService;
import utescore.repository.*;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@org.springframework.stereotype.Service
@RequiredArgsConstructor
@Transactional
public class BookingManagementService {

    private final BookingRepository bookingRepo;
    private final FootballFieldRepository fieldRepo;
    private final CustomerRepository customerRepo;
    private final BookingServiceRepository bookingServiceRepo;
    private final BookingSportWearRepository bookingSportWearRepo;
    private final ServiceRepository serviceRepo;
    private final SportWearRepository sportWearRepo;
    private final LoyaltyRepository loyaltyRepository;

    private boolean isAvailable(Long fieldId, LocalDateTime start, LocalDateTime end) {
        // existsOverlap đã check PENDING, CONFIRMED, COMPLETED
        return !bookingRepo.existsOverlap(fieldId, start, end);
    }

    public Booking createBooking(BookingRequestDTO req) {
        FootballField field = fieldRepo.findById(req.getFieldId())
                .orElseThrow(() -> new IllegalArgumentException("Field not found"));
        Customer customer = customerRepo.findById(req.getCustomerId())
                .orElseThrow(() -> new IllegalArgumentException("Customer not found"));

        if (Boolean.FALSE.equals(field.getIsActive())) {
            throw new IllegalStateException("Field is temporarily unavailable due to maintenance");
        }

        if (!isAvailable(field.getId(), req.getStartTime(), req.getEndTime())) {
            throw new IllegalStateException("Time slot is not available");
        }

        Booking b = new Booking();
        b.setField(field);
        b.setCustomer(customer);
        b.setStartTime(req.getStartTime());
        b.setEndTime(req.getEndTime());
        b.setBookingCode("BK-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase());
        b.setStatus(Booking.BookingStatus.PENDING);

        BigDecimal total = calcFieldCost(field, req.getStartTime(), req.getEndTime());
        b.setTotalAmount(total);

        Booking saved = bookingRepo.save(b);

        if (req.getServiceIds() != null) {
            for (Long sid : req.getServiceIds()) {
                utescore.entity.Service s = serviceRepo.findById(sid).orElse(null);
                if (s != null && Boolean.TRUE.equals(s.getIsAvailable())) {
                    utescore.entity.BookingService bs = new BookingService();
                    bs.setBooking(saved);
                    bs.setService(s);
                    bs.setQuantity(1);
                    bs.setUnitPrice(s.getPrice());
                    bs.setTotalPrice(s.getPrice());
                    bookingServiceRepo.save(bs);

                    saved.setTotalAmount(saved.getTotalAmount().add(s.getPrice()));
                }
            }
        }

        if (req.getSportWearIds() != null) {
            for (Long wid : req.getSportWearIds()) {
                SportWear w = sportWearRepo.findById(wid).orElse(null);
                if (w != null && Boolean.TRUE.equals(w.getIsAvailableForRent())) {
                    BookingSportWear bw = new BookingSportWear();
                    bw.setBooking(saved);
                    bw.setSportWear(w);
                    bw.setQuantity(1);
                    bw.setRentalDays(1);
                    bw.setUnitPrice(w.getRentalPricePerDay());
                    bw.setTotalPrice(w.getRentalPricePerDay());
                    bookingSportWearRepo.save(bw);

                    saved.setTotalAmount(saved.getTotalAmount().add(w.getRentalPricePerDay()));
                }
            }
        }

        return bookingRepo.save(saved);
    }

    private BigDecimal calcFieldCost(FootballField field, LocalDateTime start, LocalDateTime end) {
        long hours = Math.max(1, Duration.between(start, end).toHours());
        return field.getPricePerHour().multiply(BigDecimal.valueOf(hours));
    }

    public Booking confirm(Long bookingId) {
        Booking b = bookingRepo.findById(bookingId)
                .orElseThrow(() -> new IllegalArgumentException("Booking not found"));

        // Chỉ cho phép confirm nếu đang PENDING
        if (b.getStatus() != Booking.BookingStatus.PENDING) {
            throw new IllegalStateException("Can only confirm PENDING bookings");
        }

        b.setStatus(Booking.BookingStatus.CONFIRMED);
        return bookingRepo.save(b);
    }

    public Booking cancel(Long bookingId) {
        Booking b = bookingRepo.findById(bookingId)
                .orElseThrow(() -> new IllegalArgumentException("Booking not found"));

        // Chỉ cho phép hủy PENDING hoặc CONFIRMED
        if (b.getStatus() != Booking.BookingStatus.PENDING
                && b.getStatus() != Booking.BookingStatus.CONFIRMED) {
            throw new IllegalStateException("Can only cancel PENDING or CONFIRMED bookings");
        }

        b.setStatus(Booking.BookingStatus.CANCELLED);
        return bookingRepo.save(b);
    }

    public List<Booking> listAll() {
        return bookingRepo.findAll();
    }

    public Booking complete(Long bookingId) {
        Booking b = bookingRepo.findById(bookingId)
                .orElseThrow(() -> new IllegalArgumentException("Booking not found"));

        // Chỉ cho phép complete khi CONFIRMED
        if (b.getStatus() != Booking.BookingStatus.CONFIRMED) {
            throw new IllegalStateException("Can only complete CONFIRMED bookings");
        }

        // Đánh dấu booking hoàn tất
        b.setStatus(Booking.BookingStatus.COMPLETED);

        // ✅ Hoàn trả tất cả đồ thể thao đã mượn
        returnAllSportWears(b);

        // ✅ Nếu là CASH/COD và payment vẫn PENDING → Chuyển sang COMPLETED + tích điểm
        if (b.getPayment() != null && b.getPayment().getStatus() == Payment.PaymentStatus.PENDING) {
            Payment.PaymentMethod method = b.getPayment().getPaymentMethod();

            if (method == Payment.PaymentMethod.CASH || method == Payment.PaymentMethod.COD) {
                b.getPayment().setStatus(Payment.PaymentStatus.COMPLETED);
                b.getPayment().setPaidAt(LocalDateTime.now());

                // Tích điểm cho CASH/COD khi hoàn tất
                updateLoyaltyPoints(b.getPayment());
            }
        }

        return bookingRepo.save(b);
    }

    /**
     * Hoàn trả tất cả đồ thể thao đã mượn của booking
     */
    private void returnAllSportWears(Booking booking) {
        if (booking.getBookingSportWears() == null || booking.getBookingSportWears().isEmpty()) {
            return;
        }

        for (BookingSportWear bsw : booking.getBookingSportWears()) {
            // Chỉ hoàn trả những đồ đang ở trạng thái RENTED
            if (bsw.getStatus() == BookingSportWear.RentalStatus.RENTED) {
                bsw.setStatus(BookingSportWear.RentalStatus.RETURNED);
                bookingSportWearRepo.save(bsw);

                // Tùy chọn: Cập nhật lại số lượng tồn kho (nếu cần)
                 SportWear sportWear = bsw.getSportWear();
                 sportWear.setStockQuantity(sportWear.getStockQuantity() + bsw.getQuantity());
                 sportWearRepo.save(sportWear);
            }
        }
    }

    /**
     * Helper method để tích điểm
     */
    private void updateLoyaltyPoints(Payment payment) {
        if (payment.getBooking() == null) return;

        Customer customer = payment.getBooking().getCustomer();
        if (customer == null) return;

        Loyalty loyalty = loyaltyRepository.findByCustomer_Id(customer.getId())
                .orElseGet(() -> {
                    Loyalty newLoyalty = new Loyalty();
                    newLoyalty.setCustomer(customer);
                    return loyaltyRepository.save(newLoyalty);
                });

        int pointsToAdd = payment.getAmount().divide(BigDecimal.valueOf(1000)).intValue();

        loyalty.addPoints(pointsToAdd);
        loyalty.setTotalSpent(loyalty.getTotalSpent().add(payment.getAmount()));
        loyalty.setTotalBookings(loyalty.getTotalBookings() + 1);

        loyaltyRepository.save(loyalty);
    }
}