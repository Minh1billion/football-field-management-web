package utescore.service;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.ui.Model;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import utescore.dto.CartDTO;
import utescore.dto.RentalDTO;
import utescore.entity.*;
import utescore.entity.BookingService;
import utescore.entity.RentalOrder;
import utescore.repository.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class RentalService {
    private final RentalRepository rentalRepository;
    private final BookingRepository bookingRepository;
    private final SportWearRepository sportWearRepository;
    private final AccountRepository accountRepository;
    private final RentalOrderRepository rentalOrderRepository;
    private final RentalOrderDetailRepository rentalOrderDetailRepository;
    private final SportWearService sportWearService;
    private final ServiceService serviceService;

    public long countActiveRentals(String username) {
        return rentalRepository.countActiveRentals(username);
    }

    public Iterable<SportWear> getAvailableSportWearsForRent(Pageable pageable) {
        return sportWearService.findAvailableForRent(pageable);
    }

    public String showSportWearDetail(Long id, Model model, RedirectAttributes redirectAttributes) {
        SportWear wear = sportWearService.findById(id);
        if (wear == null) {
            redirectAttributes.addFlashAttribute("errorMessage", "Không tìm thấy sản phẩm.");
            return "redirect:/user/rentals";
        }
        model.addAttribute("sportWear", wear);
        return "user/rentals/detail";
    }

    public String addToCart(CartDTO cartDTO, Long sportWearId, int quantity, int rentalDays, RedirectAttributes redirectAttributes) {
        SportWear wear = sportWearService.findById(sportWearId);

        if (wear == null || !wear.getIsAvailableForRent()) {
            redirectAttributes.addFlashAttribute("errorMessage", "Sản phẩm không tồn tại hoặc không cho thuê.");
            return "redirect:/user/rentals/detail/" + sportWearId;
        }

        if (quantity <= 0 || rentalDays <= 0) {
            redirectAttributes.addFlashAttribute("errorMessage", "Số lượng và số ngày thuê phải lớn hơn 0.");
            return "redirect:/user/rentals/detail/" + sportWearId;
        }

        if (quantity > wear.getStockQuantity()) {
            redirectAttributes.addFlashAttribute("errorMessage",
                    "Số lượng yêu cầu vượt quá số lượng tồn kho (" + wear.getStockQuantity() + ").");
            return "redirect:/user/rentals/detail/" + sportWearId;
        }

        RentalDTO newItem = new RentalDTO();
        newItem.setSportWearId(sportWearId);
        newItem.setName(wear.getName());
        newItem.setRentalPricePerDay(wear.getRentalPricePerDay());
        newItem.setQuantity(quantity);
        newItem.setRentalDays(rentalDays);

        updateCart(cartDTO, newItem);

        redirectAttributes.addFlashAttribute("successMessage",
                "Đã thêm " + quantity + " x " + wear.getName() + " vào giỏ hàng thuê.");

        return "redirect:/user/rentals/cart";
    }

    public void updateCartItem(CartDTO cart, Long sportWearId, int quantity, int rentalDays) {
        Optional<RentalDTO> itemOpt = cart.findRentalItemById(sportWearId);
        if (itemOpt.isPresent()) {
            RentalDTO item = itemOpt.get();
            item.setQuantity(quantity);
            item.setRentalDays(rentalDays);
            recalculateCartTotal(cart);
        }
    }

    private void updateCart(CartDTO cart, RentalDTO newItem) {
        Optional<RentalDTO> existingItemOpt = cart.findRentalItemById(newItem.getSportWearId());

        if (existingItemOpt.isPresent()) {
            RentalDTO existingItem = existingItemOpt.get();
            existingItem.setQuantity(existingItem.getQuantity() + newItem.getQuantity());
            existingItem.setRentalDays(newItem.getRentalDays());
        } else {
            cart.getItems().add(newItem);
        }

        recalculateCartTotal(cart);
    }

    public void recalculateCartTotal(CartDTO cart) {
        BigDecimal total = cart.getItems().stream()
                .map(this::calculateItemSubtotal)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        cart.setTotalPrice(total);
    }

    private BigDecimal calculateItemSubtotal(RentalDTO item) {
        return item.getRentalPricePerDay()
                .multiply(BigDecimal.valueOf(item.getRentalDays()))
                .multiply(BigDecimal.valueOf(item.getQuantity()));
    }

    @Transactional
    public Long createRentalOrder(CartDTO cartDTO, String username, String customerName,
                                  String customerPhone, String customerAddress, String notes,
                                  String paymentMethod) {
        // Validate cart
        if (cartDTO == null || cartDTO.getItems() == null || cartDTO.getItems().isEmpty()) {
            throw new RuntimeException("Giỏ hàng trống");
        }

        // Lấy thông tin account
        Account account = accountRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy tài khoản"));

        // Lấy customer từ account
        Customer customer = account.getCustomer();
        if (customer == null) {
            throw new RuntimeException("Không tìm thấy thông tin khách hàng");
        }

        // Tạo đơn hàng thuê (RentalOrder)
        RentalOrder rentalOrder = new RentalOrder();
        rentalOrder.setAccount(account);
        rentalOrder.setCustomer(customer);
        rentalOrder.setCustomerName(customerName != null && !customerName.isBlank()
                ? customerName
                : customer.getFullName());
        rentalOrder.setCustomerPhone(customerPhone != null && !customerPhone.isBlank()
                ? customerPhone
                : customer.getPhoneNumber());
        rentalOrder.setCustomerAddress(customerAddress);
        rentalOrder.setOrderDate(LocalDateTime.now());

        // Validate và tính tổng tiền
        BigDecimal totalAmount = BigDecimal.ZERO;
        for (var cartItem : cartDTO.getItems()) {
            SportWear sportWear = sportWearRepository.findById(cartItem.getSportWearId())
                    .orElseThrow(() -> new RuntimeException("Không tìm thấy sản phẩm ID: " + cartItem.getSportWearId()));

            // Kiểm tra tồn kho
            if (cartItem.getQuantity() > sportWear.getStockQuantity()) {
                throw new RuntimeException("Sản phẩm " + sportWear.getName() +
                        " không đủ số lượng trong kho (Còn: " + sportWear.getStockQuantity() + ")");
            }

            // Kiểm tra còn cho thuê không
            if (!sportWear.getIsAvailableForRent()) {
                throw new RuntimeException("Sản phẩm " + sportWear.getName() + " hiện không cho thuê");
            }

            // Tính tổng tiền
            BigDecimal itemTotal = sportWear.getRentalPricePerDay()
                    .multiply(BigDecimal.valueOf(cartItem.getQuantity()))
                    .multiply(BigDecimal.valueOf(cartItem.getRentalDays()));
            totalAmount = totalAmount.add(itemTotal);
        }

        // Tạo Payment
        Payment payment = new Payment();
        payment.setNotes(notes);
        payment.setAmount(totalAmount);

        // Set payment method
        if ("COD".equals(paymentMethod)) {
            payment.setPaymentMethod(Payment.PaymentMethod.COD);
            payment.setStatus(Payment.PaymentStatus.PENDING);
            payment.setPaymentCode("COD-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase());
        } else if ("VNPAY".equals(paymentMethod)) {
            payment.setPaymentMethod(Payment.PaymentMethod.VNPAY);
            payment.setStatus(Payment.PaymentStatus.COMPLETED);
            payment.setPaymentCode("VNPAY-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase());
            payment.setPaidAt(LocalDateTime.now());
        } else {
            throw new RuntimeException("Phương thức thanh toán không hợp lệ: " + paymentMethod);
        }

        payment.setCreatedAt(LocalDateTime.now());

        // Link payment với rentalOrder
        rentalOrder.setPayment(payment);
        payment.setRentalOrder(rentalOrder);

        // Save rentalOrder (cascade sẽ save payment)
        rentalOrder = rentalOrderRepository.save(rentalOrder);

        // Tạo chi tiết đơn hàng (RentalOrderDetail)
        for (var cartItem : cartDTO.getItems()) {
            SportWear sportWear = sportWearRepository.findById(cartItem.getSportWearId())
                    .orElseThrow(() -> new RuntimeException("Không tìm thấy sản phẩm ID: " + cartItem.getSportWearId()));

            RentalOrderDetail detail = new RentalOrderDetail();
            detail.setRentalOrder(rentalOrder);
            detail.setSportWear(sportWear);
            detail.setQuantity(cartItem.getQuantity());
            detail.setRentalDays(cartItem.getRentalDays());
            detail.setRentalPricePerDay(sportWear.getRentalPricePerDay().doubleValue());

            // ✅ Tính subtotal đúng: giá × số lượng × số ngày
            double subtotal = sportWear.getRentalPricePerDay().doubleValue()
                    * cartItem.getQuantity()
                    * cartItem.getRentalDays();
            detail.setSubTotal(subtotal);

            rentalOrderDetailRepository.save(detail);

            // Giảm số lượng tồn kho
            sportWear.setStockQuantity(sportWear.getStockQuantity() - cartItem.getQuantity());
            sportWearRepository.save(sportWear);
        }

        return rentalOrder.getId();
    }

    @Transactional
    public void addSportWearToBooking(long bookingId, List<RentalDTO> rentals) {
        Booking booking = bookingRepository.findById(bookingId)
                .orElseThrow(() -> new RuntimeException("Booking không tồn tại"));

        // Kiểm tra trạng thái booking
        if (booking.getStatus() != Booking.BookingStatus.PENDING) {
            throw new RuntimeException("Chỉ có thể thêm đồ thuê khi booking đang ở trạng thái PENDING");
        }

        BigDecimal additionalAmount = BigDecimal.ZERO;

        for (RentalDTO rental : rentals) {
            SportWear wear = sportWearService.findById(rental.getSportWearId());
            if (wear == null) {
                throw new RuntimeException("Không tìm thấy sản phẩm ID: " + rental.getSportWearId());
            }

            // Kiểm tra tồn kho
            if (rental.getQuantity() > wear.getStockQuantity()) {
                throw new RuntimeException("Sản phẩm " + wear.getName() +
                        " không đủ số lượng (Còn: " + wear.getStockQuantity() + ")");
            }

            // Kiểm tra còn cho thuê không
            if (!wear.getIsAvailableForRent()) {
                throw new RuntimeException("Sản phẩm " + wear.getName() + " hiện không cho thuê");
            }

            // Kiểm tra xem đã có trong booking chưa
            boolean alreadyExists = booking.getBookingSportWears().stream()
                    .anyMatch(bsw -> bsw.getSportWear().getId().equals(rental.getSportWearId()));

            if (alreadyExists) {
                throw new RuntimeException("Sản phẩm " + wear.getName() + " đã có trong booking");
            }

            // Lấy số ngày thuê (mặc định 1 nếu không có)
            int rentalDays = rental.getRentalDays() != null && rental.getRentalDays() > 0
                    ? rental.getRentalDays()
                    : 1;

            // Tạo BookingSportWear
            BookingSportWear bsw = new BookingSportWear();
            bsw.setBooking(booking);
            bsw.setSportWear(wear);
            bsw.setQuantity(rental.getQuantity());
            bsw.setRentalDays(rentalDays);
            bsw.setUnitPrice(wear.getRentalPricePerDay());

            // ✅ FIX: Tính đúng công thức = giá × số lượng × số ngày
            BigDecimal totalPrice = wear.getRentalPricePerDay()
                    .multiply(BigDecimal.valueOf(rental.getQuantity()))
                    .multiply(BigDecimal.valueOf(rentalDays));
            bsw.setTotalPrice(totalPrice);
            bsw.setStatus(BookingSportWear.RentalStatus.RENTED);

            booking.getBookingSportWears().add(bsw);
            additionalAmount = additionalAmount.add(totalPrice);

            // Trừ số lượng trong kho
            wear.setStockQuantity(wear.getStockQuantity() - rental.getQuantity());
            sportWearRepository.save(wear);
        }

        // Cập nhật tổng tiền booking
        booking.setTotalAmount(booking.getTotalAmount().add(additionalAmount));

        // Cập nhật payment amount nếu có
        if (booking.getPayment() != null) {
            booking.getPayment().setAmount(booking.getTotalAmount());
        }

        bookingRepository.save(booking);
    }

    @Transactional
    public void addServiceToBooking(long bookingId, List<RentalDTO> rentals) {
        Booking booking = bookingRepository.findById(bookingId)
                .orElseThrow(() -> new RuntimeException("Booking không tồn tại"));

        // Kiểm tra trạng thái booking
        if (booking.getStatus() != Booking.BookingStatus.PENDING) {
            throw new RuntimeException("Chỉ có thể thêm dịch vụ khi booking đang ở trạng thái PENDING");
        }

        BigDecimal additionalAmount = BigDecimal.ZERO;

        for (RentalDTO rental : rentals) {
            utescore.entity.Service serv = serviceService.findById(rental.getServiceId());
            if (serv == null) {
                throw new RuntimeException("Không tìm thấy dịch vụ ID: " + rental.getServiceId());
            }

            // Kiểm tra service có available không
            if (!serv.getIsAvailable()) {
                throw new RuntimeException("Dịch vụ " + serv.getName() + " hiện không khả dụng");
            }

            // Kiểm tra xem đã có trong booking chưa
            boolean alreadyExists = booking.getBookingServices().stream()
                    .anyMatch(bs -> bs.getService().getId().equals(rental.getServiceId()));

            if (alreadyExists) {
                throw new RuntimeException("Dịch vụ " + serv.getName() + " đã có trong booking");
            }

            // Lấy số lượng (mặc định 1)
            int quantity = rental.getQuantity() != null && rental.getQuantity() > 0
                    ? rental.getQuantity()
                    : 1;

            BookingService bs = new BookingService();
            bs.setBooking(booking);
            bs.setService(serv);
            bs.setQuantity(quantity);
            bs.setUnitPrice(serv.getPrice());

            // ✅ Tính đúng: giá × số lượng
            BigDecimal totalPrice = serv.getPrice()
                    .multiply(BigDecimal.valueOf(quantity));
            bs.setTotalPrice(totalPrice);

            booking.getBookingServices().add(bs);
            additionalAmount = additionalAmount.add(totalPrice);
        }

        // Cập nhật tổng tiền booking
        booking.setTotalAmount(booking.getTotalAmount().add(additionalAmount));

        // Cập nhật payment amount nếu có
        if (booking.getPayment() != null) {
            booking.getPayment().setAmount(booking.getTotalAmount());
        }

        bookingRepository.save(booking);
    }
}