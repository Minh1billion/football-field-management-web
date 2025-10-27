package utescore.service;

import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import utescore.dto.BookingDTO;
import utescore.dto.RentalDTO;
import utescore.dto.TimeSlotDTO;
import utescore.entity.*;
import utescore.repository.*;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional
public class BookingService {

    private final BookingRepository bookingRepo;
    private final FootballFieldRepository fieldRepo;
    private final AccountRepository accountRepo;
    private final CustomerRepository customerRepo;
    private final FieldAvailabilityRepository availabilityRepo;
    private final MaintenanceRepository maintenanceRepo;
    private final ServiceRepository serviceRepo;
    private final SportWearRepository sportWearRepo;
    private final BookingSportWearRepository bookingSportWearRepo;
    private final LoyaltyRepository loyaltyRepo;

    // ===== PHƯƠNG THỨC TÍNH DISCOUNT THEO TIER =====
    private BigDecimal getDiscountRate(Loyalty.MembershipTier tier) {
        if (tier == null) {
            return BigDecimal.ZERO;
        }

        return switch (tier) {
            case BRONZE -> BigDecimal.ZERO;
            case SILVER -> new BigDecimal("0.05"); // 5%
            case GOLD -> new BigDecimal("0.10");   // 10%
            case PLATINUM -> new BigDecimal("0.15"); // 15%
        };
    }

    private BigDecimal applyDiscount(BigDecimal amount, BigDecimal discountRate) {
        if (discountRate.compareTo(BigDecimal.ZERO) == 0) {
            return amount;
        }

        BigDecimal discount = amount.multiply(discountRate);
        return amount.subtract(discount).setScale(2, RoundingMode.HALF_UP);
    }

    // ===== TÍNH ĐIỂM THƯỞNG =====
    private int calculatePointsFromAmount(BigDecimal amount) {
        // 1 điểm cho mỗi 1,000 VND
        return amount.divide(new BigDecimal("1000"), 0, RoundingMode.DOWN).intValue();
    }

    public long countUpcomingBookings(String username) {
        return bookingRepo.countUpcomingBookings(username, LocalDateTime.now());
    }

    public long calculateMonthlySpending(String username) {
        Long spending = bookingRepo.calculateMonthlySpending(username, LocalDateTime.now().getMonthValue(), LocalDateTime.now().getYear());
        return spending != null ? spending : 0L;
    }

    public List<BookingDTO> getAllBookings() {
        return bookingRepo.findAll().stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }

    public List<BookingDTO> getMyBooking() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String username = authentication.getName();
        return bookingRepo.findByCustomer_Account_UsernameOrderByCreatedAt(username).stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }

    public List<BookingDTO> getBookingsByUsername(String username) {
        Account user = accountRepo.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("User not found"));

        List<Booking> bookings = bookingRepo.findByCustomer_Account_UsernameOrderByCreatedAt(user.getUsername());
        return bookings.stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public BookingDTO getBookingById(Long id) {
        Booking booking = bookingRepo.findById(id)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy booking"));

        BookingDTO dto = new BookingDTO();
        dto.setId(booking.getId());
        dto.setBookingCode(booking.getBookingCode());
        dto.setFieldId(booking.getField().getId());
        dto.setFieldName(booking.getField().getName());
        dto.setCustomerId(booking.getCustomer().getId());
        dto.setCustomerName(booking.getCustomer().getFullName());
        dto.setStartTime(booking.getStartTime());
        dto.setEndTime(booking.getEndTime());
        dto.setNotes(booking.getNotes());
        dto.setTotalAmount(booking.getTotalAmount());
        dto.setStatus(booking.getStatus().toString());

        if (booking.getPayment() != null) {
            dto.setPaymentMethod(booking.getPayment().getPaymentMethod().toString());
        }

        List<RentalDTO> sportWears = booking.getBookingSportWears().stream()
                .map(bsw -> {
                    RentalDTO rental = new RentalDTO();
                    rental.setSportWearId(bsw.getSportWear().getId());
                    rental.setName(bsw.getSportWear().getName());
                    rental.setQuantity(bsw.getQuantity());
                    rental.setRentalDays(bsw.getRentalDays());
                    rental.setRentalPricePerDay(bsw.getUnitPrice());
                    rental.setTotalPrice(bsw.getTotalPrice());
                    rental.setStatus(bsw.getStatus().toString());
                    return rental;
                })
                .toList();
        dto.setSportWears(sportWears);

        List<RentalDTO> services = booking.getBookingServices().stream()
                .map(bs -> {
                    RentalDTO rental = new RentalDTO();
                    rental.setServiceId(bs.getService().getId());
                    rental.setName(bs.getService().getName());
                    rental.setQuantity(bs.getQuantity());
                    rental.setRentalPricePerDay(bs.getUnitPrice());
                    rental.setTotalPrice(bs.getTotalPrice());
                    return rental;
                })
                .toList();
        dto.setServices(services);

        return dto;
    }

    public BookingDTO createBooking(BookingDTO dto, String username) {
        Customer customer = customerRepo.findByAccount_Username(username);

        if (customer == null) {
            throw new RuntimeException("Customer not found");
        }

        dto.setCustomerId(customer.getId());

        // Lấy thông tin loyalty để tính discount
        Loyalty loyalty = loyaltyRepo.findByCustomer_Account_Username(username);
        Loyalty.MembershipTier tier = loyalty != null ? loyalty.getTier() : Loyalty.MembershipTier.BRONZE;
        BigDecimal discountRate = getDiscountRate(tier);

        // Validate field
        FootballField field = fieldRepo.findById(dto.getFieldId())
                .orElseThrow(() -> new RuntimeException("Field not found"));

        // Chặn đặt sân khi sân đang tạm ngừng
        if (Boolean.FALSE.equals(field.getIsActive())) {
            throw new RuntimeException("Field is temporarily unavailable due to maintenance");
        }

        // Parse thời gian
        LocalDateTime startDateTime = dto.getStartTime();
        LocalDateTime endDateTime = dto.getEndTime();

        // Validate thời gian
        if (startDateTime == null || endDateTime == null) {
            throw new RuntimeException("Start time and end time are required");
        }

        if (!startDateTime.isBefore(endDateTime)) {
            throw new RuntimeException("End time must be after start time");
        }

        // Kiểm tra overlap bookings
        boolean hasOverlap = bookingRepo.existsOverlap(field.getId(), startDateTime, endDateTime);
        if (hasOverlap) {
            throw new RuntimeException("Time slot is already booked");
        }

        // Kiểm tra availability windows
        boolean isBlocked = !availabilityRepo.findBlockingWindows(
                field.getId(), startDateTime, endDateTime
        ).isEmpty();
        if (isBlocked) {
            throw new RuntimeException("Time slot is blocked");
        }

        // Kiểm tra maintenance kế hoạch trong khoảng
        boolean hasMaintenance = !maintenanceRepo.findPlannedInRange(
                field.getId(), startDateTime, endDateTime
        ).isEmpty();
        if (hasMaintenance) {
            throw new RuntimeException("Field is under maintenance");
        }

        // Tính toán giá sân
        long hours = java.time.Duration.between(startDateTime, endDateTime).toHours();
        if (hours <= 0) {
            hours = 1; // Tối thiểu 1 giờ
        }

        BigDecimal fieldPrice = field.getPricePerHour()
                .multiply(BigDecimal.valueOf(hours));

        // Tạo booking NHƯNG CHƯA SAVE
        Booking booking = new Booking();
        booking.setCustomer(customer);
        booking.setBookingCode("BK-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase());
        booking.setField(field);
        booking.setStartTime(startDateTime);
        booking.setEndTime(endDateTime);
        booking.setStatus(Booking.BookingStatus.PENDING);
        booking.setNotes(dto.getNotes());
        booking.setCreatedAt(LocalDateTime.now());
        booking.setTotalAmount(fieldPrice); // Set giá sân tạm thời để tránh NULL

        // Tổng tiền trước giảm giá
        BigDecimal subtotal = fieldPrice;

        // ===== XỬ LÝ SERVICES =====
        if (dto.getServices() != null && !dto.getServices().isEmpty()) {
            BigDecimal serviceTotal = BigDecimal.ZERO;

            for (RentalDTO serviceDTO : dto.getServices()) {
                if (serviceDTO.getQuantity() != null && serviceDTO.getQuantity() > 0) {
                    utescore.entity.Service service = serviceRepo.findById(serviceDTO.getServiceId())
                            .orElseThrow(() -> new RuntimeException("Service not found: " + serviceDTO.getServiceId()));

                    if (!service.getIsAvailable()) {
                        throw new RuntimeException("Service is not available: " + service.getName());
                    }

                    utescore.entity.BookingService bookingService = new utescore.entity.BookingService();
                    bookingService.setBooking(booking);
                    bookingService.setService(service);
                    bookingService.setQuantity(serviceDTO.getQuantity());
                    bookingService.setUnitPrice(service.getPrice());

                    BigDecimal itemTotal = service.getPrice()
                            .multiply(BigDecimal.valueOf(serviceDTO.getQuantity()));
                    bookingService.setTotalPrice(itemTotal);

                    booking.getBookingServices().add(bookingService);
                    serviceTotal = serviceTotal.add(itemTotal);
                }
            }

            subtotal = subtotal.add(serviceTotal);
        }

        // ===== XỬ LÝ SPORTWEARS =====
        if (dto.getSportWears() != null && !dto.getSportWears().isEmpty()) {
            BigDecimal sportWearTotal = BigDecimal.ZERO;

            for (RentalDTO sportWearDTO : dto.getSportWears()) {
                if (sportWearDTO.getQuantity() != null && sportWearDTO.getQuantity() > 0) {
                    SportWear sportWear = sportWearRepo.findById(sportWearDTO.getSportWearId())
                            .orElseThrow(() -> new RuntimeException("SportWear not found: " + sportWearDTO.getSportWearId()));

                    if (sportWear.getStockQuantity() < sportWearDTO.getQuantity()) {
                        throw new RuntimeException("Not enough stock for " + sportWear.getName() +
                                ". Available: " + sportWear.getStockQuantity());
                    }

                    int rentalDays = sportWearDTO.getRentalDays() != null && sportWearDTO.getRentalDays() > 0
                            ? sportWearDTO.getRentalDays()
                            : 1;

                    BookingSportWear bookingSportWear = new BookingSportWear();
                    bookingSportWear.setBooking(booking);
                    bookingSportWear.setSportWear(sportWear);
                    bookingSportWear.setQuantity(sportWearDTO.getQuantity());
                    bookingSportWear.setRentalDays(rentalDays);
                    bookingSportWear.setUnitPrice(sportWear.getRentalPricePerDay());

                    BigDecimal itemTotal = sportWear.getRentalPricePerDay()
                            .multiply(BigDecimal.valueOf(sportWearDTO.getQuantity()))
                            .multiply(BigDecimal.valueOf(rentalDays));
                    bookingSportWear.setTotalPrice(itemTotal);
                    bookingSportWear.setStatus(BookingSportWear.RentalStatus.RENTED);

                    booking.getBookingSportWears().add(bookingSportWear);
                    sportWearTotal = sportWearTotal.add(itemTotal);

                    // Trừ số lượng trong kho
                    sportWear.setStockQuantity(sportWear.getStockQuantity() - sportWearDTO.getQuantity());
                    sportWearRepo.save(sportWear);
                }
            }

            subtotal = subtotal.add(sportWearTotal);
        }

        // ===== ÁP DỤNG GIẢM GIÁ THEO TIER =====
        BigDecimal finalAmount = applyDiscount(subtotal, discountRate);
        booking.setTotalAmount(finalAmount);

        // ===== LƯU BOOKING TRƯỚC KHI TẠO PAYMENT =====
        booking = bookingRepo.save(booking);

        // ===== TẠO PAYMENT =====
        Payment payment = new Payment();
        payment.setBooking(booking);

        String method = dto.getPaymentMethod();
        if (method == null || method.isBlank()) {
            method = "CASH";
        }
        method = method.toUpperCase().replace("-", "_");

        Payment.PaymentMethod paymentMethod;
        try {
            paymentMethod = Payment.PaymentMethod.valueOf(method);
        } catch (IllegalArgumentException e) {
            throw new RuntimeException("Phương thức thanh toán không hợp lệ: " + dto.getPaymentMethod());
        }

        payment.setPaymentMethod(paymentMethod);
        payment.setCreatedAt(LocalDateTime.now());
        payment.setAmount(finalAmount);
        payment.setStatus(Payment.PaymentStatus.PENDING);
        payment.setNotes(dto.getNotes());
        payment.setPaymentCode(paymentMethod.name() + "-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase());

        booking.setPayment(payment);
        bookingRepo.save(booking);

        // ===== TÍCH ĐIỂM THƯỞNG =====
        if (loyalty != null) {
            int earnedPoints = calculatePointsFromAmount(finalAmount);
            loyalty.addPoints(earnedPoints);
            loyalty.setTotalSpent(loyalty.getTotalSpent().add(finalAmount));
            loyalty.setTotalBookings(loyalty.getTotalBookings() + 1);
            loyaltyRepo.save(loyalty);
        }

        return convertToDTO(booking);
    }

    @Transactional
    public void updateBookingServices(BookingDTO dto) {
        Booking booking = bookingRepo.findById(dto.getId())
                .orElseThrow(() -> new RuntimeException("Booking not found"));

        if (booking.getStatus() != Booking.BookingStatus.PENDING &&
                booking.getStatus() != Booking.BookingStatus.CONFIRMED) {
            throw new RuntimeException("Cannot update completed or cancelled booking");
        }

        // Lấy loyalty để tính discount
        Customer customer = booking.getCustomer();
        Loyalty loyalty = loyaltyRepo.findByCustomer_Account_Username(customer.getAccount().getUsername());
        Loyalty.MembershipTier tier = loyalty != null ? loyalty.getTier() : Loyalty.MembershipTier.BRONZE;
        BigDecimal discountRate = getDiscountRate(tier);

        // Xóa tất cả services cũ
        booking.getBookingServices().clear();
        bookingRepo.save(booking);

        // Tính lại giá sân
        long hours = java.time.Duration.between(booking.getStartTime(), booking.getEndTime()).toHours();
        if (hours <= 0) {
            hours = 1;
        }

        BigDecimal fieldPrice = booking.getField().getPricePerHour()
                .multiply(BigDecimal.valueOf(hours));

        BigDecimal serviceTotal = BigDecimal.ZERO;

        // Thêm services mới
        if (dto.getServices() != null && !dto.getServices().isEmpty()) {
            for (RentalDTO serviceDTO : dto.getServices()) {
                if (serviceDTO.getQuantity() != null && serviceDTO.getQuantity() > 0) {
                    utescore.entity.Service service = serviceRepo.findById(serviceDTO.getServiceId())
                            .orElseThrow(() -> new RuntimeException("Service not found: " + serviceDTO.getServiceId()));

                    if (!service.getIsAvailable()) {
                        throw new RuntimeException("Service is not available: " + service.getName());
                    }

                    utescore.entity.BookingService bookingService = new utescore.entity.BookingService();
                    bookingService.setBooking(booking);
                    bookingService.setService(service);
                    bookingService.setQuantity(serviceDTO.getQuantity());
                    bookingService.setUnitPrice(service.getPrice());

                    BigDecimal itemTotal = service.getPrice()
                            .multiply(BigDecimal.valueOf(serviceDTO.getQuantity()));
                    bookingService.setTotalPrice(itemTotal);

                    booking.getBookingServices().add(bookingService);
                    serviceTotal = serviceTotal.add(itemTotal);
                }
            }
        }

        // Tính tổng sportwear
        BigDecimal sportWearTotal = booking.getBookingSportWears().stream()
                .map(BookingSportWear::getTotalPrice)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        // Tính subtotal
        BigDecimal subtotal = fieldPrice.add(serviceTotal).add(sportWearTotal);

        // Áp dụng giảm giá
        BigDecimal finalAmount = applyDiscount(subtotal, discountRate);
        booking.setTotalAmount(finalAmount);

        // Cập nhật payment
        if (booking.getPayment() != null) {
            booking.getPayment().setAmount(finalAmount);
        }

        if (dto.getNotes() != null) {
            booking.setNotes(dto.getNotes());
        }

        bookingRepo.save(booking);
    }

    public void cancelBooking(Long bookingId) {
        Booking booking = bookingRepo.findById(bookingId)
                .orElseThrow(() -> new RuntimeException("Booking not found"));

        if (booking.getStatus() != Booking.BookingStatus.PENDING &&
                booking.getStatus() != Booking.BookingStatus.CONFIRMED) {
            throw new RuntimeException("Cannot cancel this booking");
        }

        // Hoàn trả sportwear
        for (BookingSportWear bsw : booking.getBookingSportWears()) {
            SportWear sportWear = bsw.getSportWear();
            sportWear.setStockQuantity(sportWear.getStockQuantity() + bsw.getQuantity());
            sportWearRepo.save(sportWear);
        }

        // Hoàn trả điểm loyalty nếu đã thanh toán
        if (booking.getPayment() != null &&
                booking.getPayment().getStatus() == Payment.PaymentStatus.COMPLETED) {

            Customer customer = booking.getCustomer();
            Loyalty loyalty = loyaltyRepo.findByCustomer_Account_Username(customer.getAccount().getUsername());

            if (loyalty != null) {
                int pointsToRefund = calculatePointsFromAmount(booking.getTotalAmount());
                loyalty.setPoints(Math.max(0, loyalty.getPoints() - pointsToRefund));
                loyalty.setTotalSpent(loyalty.getTotalSpent().subtract(booking.getTotalAmount()));
                loyalty.setTotalBookings(Math.max(0, loyalty.getTotalBookings() - 1));
                loyaltyRepo.save(loyalty);
            }
        }

        booking.setStatus(Booking.BookingStatus.CANCELLED);

        if (booking.getPayment() != null) {
            booking.getPayment().setStatus(Payment.PaymentStatus.CANCELLED);
        }

        bookingRepo.save(booking);
    }

    public List<TimeSlotDTO> getAvailableTimeSlots(Long fieldId, LocalDate date) {
        FootballField field = fieldRepo.findById(fieldId)
                .orElseThrow(() -> new RuntimeException("Field not found"));

        List<TimeSlotDTO> timeSlots = new ArrayList<>();

        for (int hour = 8; hour < 22; hour++) {
            LocalTime startTime = LocalTime.of(hour, 0);
            LocalTime endTime = LocalTime.of(hour + 1, 0);

            LocalDateTime startDateTime = LocalDateTime.of(date, startTime);
            LocalDateTime endDateTime = LocalDateTime.of(date, endTime);

            boolean available = isTimeSlotAvailable(fieldId, startDateTime, endDateTime);

            timeSlots.add(new TimeSlotDTO(
                    startTime.toString(),
                    endTime.toString(),
                    available
            ));
        }

        return timeSlots;
    }

    private boolean isTimeSlotAvailable(Long fieldId, LocalDateTime start, LocalDateTime end) {
        FootballField field = fieldRepo.findById(fieldId).orElse(null);
        if (field == null || Boolean.FALSE.equals(field.getIsActive())) {
            return false;
        }

        boolean hasBooking = bookingRepo.existsOverlap(fieldId, start, end);
        if (hasBooking) {
            return false;
        }

        boolean isBlocked = !availabilityRepo.findBlockingWindows(fieldId, start, end).isEmpty();
        if (isBlocked) {
            return false;
        }

        boolean hasMaintenance = !maintenanceRepo.findPlannedInRange(fieldId, start, end).isEmpty();
        if (hasMaintenance) {
            return false;
        }

        return true;
    }

    public long countBookingsByManagerAndDate(String username, LocalDate date) {
        Account manager = accountRepo.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("Manager not found"));

        LocalDateTime startOfDay = date.atStartOfDay();
        LocalDateTime endOfDay = date.plusDays(1).atStartOfDay();

        return bookingRepo.countByField_ManagerIdAndStartTimeBetween(manager.getId(), startOfDay, endOfDay);
    }

    public BigDecimal calculateRevenueByManagerAndDateRange(String username, LocalDate startDate, LocalDate endDate) {
        Account manager = accountRepo.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("Manager not found"));

        return bookingRepo.sumTotalAmountByManagerAndDateRange(manager.getId(), startDate.atStartOfDay(), endDate.plusDays(1).atStartOfDay());
    }

    public long countActiveCustomersByManagerAndMonth(String username, int month, int year) {
        Account manager = accountRepo.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("Manager not found"));

        LocalDateTime start = LocalDate.of(year, month, 1).atStartOfDay();
        LocalDateTime end = start.plusMonths(1);

        return bookingRepo.countActiveCustomersByManagerAndDateRange(
                manager.getId(),
                start,
                end
        );
    }

    public long countPendingBookingsByManager(String username) {
        Account manager = accountRepo.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("Manager not found"));
        return bookingRepo.countByField_ManagerIdAndStatus(manager.getId(), Booking.BookingStatus.PENDING);
    }

    public long countBookedSlotsByManagerAndDate(String username, LocalDate date) {
        Account manager = accountRepo.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("Manager not found"));

        LocalDateTime start = date.atStartOfDay();
        LocalDateTime end = date.plusDays(1).atStartOfDay();

        return bookingRepo.countBookedSlotsByManagerIdAndDateRange(manager.getId(), start, end);
    }

    public boolean isBookingOwner(String username, Long bookingId) {
        Booking booking = bookingRepo.findById(bookingId)
                .orElseThrow(() -> new RuntimeException("Booking not found"));

        Customer customer = customerRepo.findByAccount_Username(username);

        if (customer == null) {
            return false;
        }

        return customer.getId().equals(booking.getCustomer().getId());
    }

    private BookingDTO convertToDTO(Booking booking) {
        BookingDTO dto = new BookingDTO();
        dto.setId(booking.getId());
        dto.setBookingCode(booking.getBookingCode());
        dto.setCustomerId(booking.getCustomer().getId());
        dto.setCustomerName(booking.getCustomer().getFirstName() + " " + booking.getCustomer().getLastName());
        dto.setStatus(booking.getStatus().toString());
        if (booking.getPayment() != null) {
            dto.setPaymentStatus(booking.getPayment().getStatus().name());
        } else {
            dto.setPaymentStatus("PENDING");
        }
        dto.setFieldId(booking.getField().getId());
        dto.setFieldName(booking.getField().getName());
        dto.setBookingTime(booking.getCreatedAt() != null ? booking.getCreatedAt().toLocalDate() : LocalDate.now());
        dto.setNotes(booking.getNotes());
        dto.setStartTime(booking.getStartTime());
        dto.setEndTime(booking.getEndTime());
        dto.setTotalAmount(booking.getTotalAmount());

        if (booking.getPayment() != null && booking.getPayment().getPaymentMethod() != null) {
            dto.setPaymentMethod(booking.getPayment().getPaymentMethod().name());
        } else {
            dto.setPaymentMethod("UNPAID");
        }

        return dto;
    }

    @Transactional
    public void removeSportWearFromBooking(Long bookingId, Long sportWearId) {
        Booking booking = bookingRepo.findById(bookingId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy booking"));

        if (booking.getStatus() != Booking.BookingStatus.PENDING) {
            throw new RuntimeException("Chỉ có thể xóa đồ thuê khi booking đang chờ xác nhận");
        }

        // Lấy loyalty để tính lại discount
        Customer customer = booking.getCustomer();
        Loyalty loyalty = loyaltyRepo.findByCustomer_Account_Username(customer.getAccount().getUsername());
        Loyalty.MembershipTier tier = loyalty != null ? loyalty.getTier() : Loyalty.MembershipTier.BRONZE;
        BigDecimal discountRate = getDiscountRate(tier);

        BookingSportWear toRemove = booking.getBookingSportWears().stream()
                .filter(bsw -> bsw.getSportWear().getId().equals(sportWearId))
                .findFirst()
                .orElseThrow(() -> new RuntimeException("Không tìm thấy đồ thuê trong booking"));

        // Hoàn trả kho
        SportWear sportWear = toRemove.getSportWear();
        sportWear.setStockQuantity(sportWear.getStockQuantity() + toRemove.getQuantity());
        sportWearRepo.save(sportWear);

        // Xóa
        booking.getBookingSportWears().remove(toRemove);
        bookingSportWearRepo.delete(toRemove);

        // Tính lại subtotal
        long hours = java.time.Duration.between(booking.getStartTime(), booking.getEndTime()).toHours();
        if (hours <= 0) hours = 1;

        BigDecimal fieldPrice = booking.getField().getPricePerHour().multiply(BigDecimal.valueOf(hours));

        BigDecimal serviceTotal = booking.getBookingServices().stream()
                .map(utescore.entity.BookingService::getTotalPrice)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal sportWearTotal = booking.getBookingSportWears().stream()
                .map(BookingSportWear::getTotalPrice)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal subtotal = fieldPrice.add(serviceTotal).add(sportWearTotal);
        BigDecimal finalAmount = applyDiscount(subtotal, discountRate);

        booking.setTotalAmount(finalAmount);

        if (booking.getPayment() != null) {
            booking.getPayment().setAmount(finalAmount);
        }

        bookingRepo.save(booking);
    }
}