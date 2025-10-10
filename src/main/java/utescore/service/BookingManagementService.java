package utescore.service;

import lombok.RequiredArgsConstructor;
import org.springframework.transaction.annotation.Transactional;
import utescore.dto.BookingRequestDTO;
import utescore.entity.*;
import utescore.repository.*;

import java.math.BigDecimal;
import java.time.Duration;
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

    private boolean isAvailable(Long fieldId, java.time.LocalDateTime start, java.time.LocalDateTime end) {
        return !bookingRepo.existsOverlap(fieldId, start, end);
    }

    public Booking createBooking(BookingRequestDTO req) {
        FootballField field = fieldRepo.findById(req.getFieldId()).orElseThrow(() -> new IllegalArgumentException("Field not found"));
        Customer customer = customerRepo.findById(req.getCustomerId()).orElseThrow(() -> new IllegalArgumentException("Customer not found"));

        if (!isAvailable(field.getId(), req.getStartTime(), req.getEndTime())) {
            throw new IllegalStateException("Time slot is not available");
        }

        Booking b = new Booking();
        b.setField(field);
        b.setCustomer(customer);
        b.setStartTime(req.getStartTime());
        b.setEndTime(req.getEndTime());
        b.setBookingCode("BK-" + UUID.randomUUID().toString().substring(0,8).toUpperCase());
        b.setStatus(Booking.BookingStatus.PENDING);

        BigDecimal total = calcFieldCost(field, req.getStartTime(), req.getEndTime());
        b.setTotalAmount(total);

        Booking saved = bookingRepo.save(b);

        // Dịch vụ kèm theo (mặc định quantity=1)
        if (req.getServiceIds() != null) {
            for (Long sid : req.getServiceIds()) {
                utescore.entity.Service s = serviceRepo.findById(sid).orElse(null);
                if (s != null && Boolean.TRUE.equals(s.getIsAvailable())) {
                    BookingService bs = new BookingService();
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

        // Thuê đồ (mặc định quantity=1, rentalDays=1)
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

    private BigDecimal calcFieldCost(FootballField field, java.time.LocalDateTime start, java.time.LocalDateTime end) {
        long hours = Math.max(1, Duration.between(start, end).toHours());
        return field.getPricePerHour().multiply(BigDecimal.valueOf(hours));
    }

    public Booking confirm(Long bookingId) {
        Booking b = bookingRepo.findById(bookingId).orElseThrow(() -> new IllegalArgumentException("Booking not found"));
        b.setStatus(Booking.BookingStatus.CONFIRMED);
        return bookingRepo.save(b);
    }

    public Booking cancel(Long bookingId) {
        Booking b = bookingRepo.findById(bookingId).orElseThrow(() -> new IllegalArgumentException("Booking not found"));
        b.setStatus(Booking.BookingStatus.CANCELLED);
        return bookingRepo.save(b);
    }

    public List<Booking> listAll() {
        return bookingRepo.findAll();
    }
}