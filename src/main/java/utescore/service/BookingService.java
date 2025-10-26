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

        // Tạo booking
        Booking booking = new Booking();
        booking.setCustomer(customer);
        booking.setBookingCode("BK-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase());
        booking.setField(field);
        booking.setStartTime(startDateTime);
        booking.setEndTime(endDateTime);
        booking.setTotalAmount(fieldPrice); // Tạm thời set giá sân
        booking.setStatus(Booking.BookingStatus.PENDING);
        booking.setNotes(dto.getNotes());
        booking.setCreatedAt(LocalDateTime.now());

        booking = bookingRepo.save(booking);

        // Tổng tiền bắt đầu từ giá sân
        BigDecimal totalAmount = fieldPrice;

        // ===== XỬ LÝ SERVICES =====
        if (dto.getServices() != null && !dto.getServices().isEmpty()) {
            BigDecimal serviceTotal = BigDecimal.ZERO;

            for (RentalDTO serviceDTO : dto.getServices()) {
                if (serviceDTO.getQuantity() != null && serviceDTO.getQuantity() > 0) {
                    utescore.entity.Service service = serviceRepo.findById(serviceDTO.getServiceId())
                            .orElseThrow(() -> new RuntimeException("Service not found: " + serviceDTO.getServiceId()));

                    // Kiểm tra service có available không
                    if (!service.getIsAvailable()) {
                        throw new RuntimeException("Service is not available: " + service.getName());
                    }

                    // Tạo BookingService entity
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

            totalAmount = totalAmount.add(serviceTotal);
        }

        // ===== XỬ LÝ SPORTWEARS =====
        if (dto.getSportWears() != null && !dto.getSportWears().isEmpty()) {
            BigDecimal sportWearTotal = BigDecimal.ZERO;

            for (RentalDTO sportWearDTO : dto.getSportWears()) {
                if (sportWearDTO.getQuantity() != null && sportWearDTO.getQuantity() > 0) {
                    SportWear sportWear = sportWearRepo.findById(sportWearDTO.getSportWearId())
                            .orElseThrow(() -> new RuntimeException("SportWear not found: " + sportWearDTO.getSportWearId()));

                    // Kiểm tra tồn kho
                    if (sportWear.getStockQuantity() < sportWearDTO.getQuantity()) {
                        throw new RuntimeException("Not enough stock for " + sportWear.getName() +
                                ". Available: " + sportWear.getStockQuantity());
                    }

                    // Số ngày thuê (mặc định 1 nếu không có)
                    int rentalDays = sportWearDTO.getRentalDays() != null && sportWearDTO.getRentalDays() > 0
                            ? sportWearDTO.getRentalDays()
                            : 1;

                    // Tạo BookingSportWear entity
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

            totalAmount = totalAmount.add(sportWearTotal);
        }

        // Cập nhật tổng tiền cuối cùng
        booking.setTotalAmount(totalAmount);

        // ===== TẠO PAYMENT =====
        Payment payment = new Payment();
        payment.setBooking(booking);

        String method = dto.getPaymentMethod();
        if (method == null || method.isBlank()) {
            method = "CASH"; // default
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
        payment.setAmount(totalAmount); // ✅ FIX: Dùng totalAmount đã tính đầy đủ
        payment.setStatus(Payment.PaymentStatus.PENDING);
        payment.setNotes(dto.getNotes());
        payment.setPaymentCode(paymentMethod.name() + "-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase());

        booking.setPayment(payment);
        bookingRepo.save(booking);

        return convertToDTO(booking);
    }

    @Transactional
    public void updateBookingServices(BookingDTO dto) {
        Booking booking = bookingRepo.findById(dto.getId())
                .orElseThrow(() -> new RuntimeException("Booking not found"));

        // Kiểm tra trạng thái
        if (booking.getStatus() != Booking.BookingStatus.PENDING &&
                booking.getStatus() != Booking.BookingStatus.CONFIRMED) {
            throw new RuntimeException("Cannot update completed or cancelled booking");
        }

        // Xóa tất cả services cũ
        booking.getBookingServices().clear();
        bookingRepo.save(booking);

        // Tính lại tổng tiền bắt đầu từ giá sân
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

        // Tính tổng tiền bao gồm cả sportwear hiện có
        BigDecimal sportWearTotal = booking.getBookingSportWears().stream()
                .map(BookingSportWear::getTotalPrice)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        // Cập nhật tổng tiền = giá sân + services + sportwears
        BigDecimal totalAmount = fieldPrice.add(serviceTotal).add(sportWearTotal);
        booking.setTotalAmount(totalAmount);

        // Cập nhật payment amount nếu có
        if (booking.getPayment() != null) {
            booking.getPayment().setAmount(totalAmount);
        }

        // Cập nhật notes nếu có
        if (dto.getNotes() != null) {
            booking.setNotes(dto.getNotes());
        }

        bookingRepo.save(booking);
    }

    public void cancelBooking(Long bookingId) {
        Booking booking = bookingRepo.findById(bookingId)
                .orElseThrow(() -> new RuntimeException("Booking not found"));

        // Chỉ cho phép hủy nếu đang ở trạng thái PENDING hoặc CONFIRMED
        if (booking.getStatus() != Booking.BookingStatus.PENDING &&
                booking.getStatus() != Booking.BookingStatus.CONFIRMED) {
            throw new RuntimeException("Cannot cancel this booking");
        }

        // Hoàn trả số lượng sportwear về kho
        for (BookingSportWear bsw : booking.getBookingSportWears()) {
            SportWear sportWear = bsw.getSportWear();
            sportWear.setStockQuantity(sportWear.getStockQuantity() + bsw.getQuantity());
            sportWearRepo.save(sportWear);
        }

        booking.setStatus(Booking.BookingStatus.CANCELLED);

        // Cập nhật payment status nếu có
        if (booking.getPayment() != null) {
            booking.getPayment().setStatus(Payment.PaymentStatus.CANCELLED);
        }

        bookingRepo.save(booking);
    }

    public List<TimeSlotDTO> getAvailableTimeSlots(Long fieldId, LocalDate date) {
        FootballField field = fieldRepo.findById(fieldId)
                .orElseThrow(() -> new RuntimeException("Field not found"));

        List<TimeSlotDTO> timeSlots = new ArrayList<>();

        // Tạo các khung giờ từ 8:00 đến 22:00, mỗi khung 1 giờ
        for (int hour = 8; hour < 22; hour++) {
            LocalTime startTime = LocalTime.of(hour, 0);
            LocalTime endTime = LocalTime.of(hour + 1, 0);

            LocalDateTime startDateTime = LocalDateTime.of(date, startTime);
            LocalDateTime endDateTime = LocalDateTime.of(date, endTime);

            // Kiểm tra xem khung giờ này có khả dụng không
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
        // Kiểm tra sân có active không
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

        // Tìm BookingSportWear cần xóa
        BookingSportWear toRemove = booking.getBookingSportWears().stream()
                .filter(bsw -> bsw.getSportWear().getId().equals(sportWearId))
                .findFirst()
                .orElseThrow(() -> new RuntimeException("Không tìm thấy đồ thuê trong booking"));

        // Lấy giá trước khi xóa
        BigDecimal removedPrice = toRemove.getTotalPrice();

        // Hoàn trả số lượng vào kho
        SportWear sportWear = toRemove.getSportWear();
        sportWear.setStockQuantity(sportWear.getStockQuantity() + toRemove.getQuantity());
        sportWearRepo.save(sportWear);

        // XÓA từ collection VÀ database
        booking.getBookingSportWears().remove(toRemove);
        bookingSportWearRepo.delete(toRemove);

        // Cập nhật tổng tiền
        booking.setTotalAmount(booking.getTotalAmount().subtract(removedPrice));

        if (booking.getPayment() != null) {
            booking.getPayment().setAmount(booking.getTotalAmount());
        }

        bookingRepo.save(booking);
    }
}