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
        // Lấy thông tin account
        Account account = accountRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy tài khoản"));

        // ⭐ LẤY CUSTOMER TỪ ACCOUNT
        Customer customer = account.getCustomer();
        if (customer == null) {
            throw new RuntimeException("Không tìm thấy thông tin khách hàng");
        }

        // Tạo đơn hàng thuê (RentalOrder)
        RentalOrder rentalOrder = new RentalOrder();
        rentalOrder.setAccount(account);
        rentalOrder.setCustomer(customer);  // ⭐ SET CUSTOMER
        rentalOrder.setCustomerName(customerName);
        rentalOrder.setCustomerPhone(customerPhone);
        rentalOrder.setCustomerAddress(customerAddress);
        rentalOrder.setOrderDate(LocalDateTime.now());

        // Tạo Payment
        Payment payment = new Payment();
        payment.setNotes(notes);
        payment.setAmount(cartDTO.getTotalPrice());
        payment.setPaymentMethod("COD".equals(paymentMethod) ? Payment.PaymentMethod.COD : Payment.PaymentMethod.VNPAY);

        // Set trạng thái payment dựa vào phương thức thanh toán
        if ("COD".equals(paymentMethod)) {
            payment.setStatus(Payment.PaymentStatus.PENDING);
            payment.setPaymentCode("COD-" + LocalDateTime.now().toString().replace(":", "").replace(".", "").substring(0, 20));
        } else if ("VNPAY".equals(paymentMethod)) {
            // VNPAY sẽ được cập nhật sau khi callback thành công
            payment.setStatus(Payment.PaymentStatus.COMPLETED);
            payment.setPaymentCode("VNPAY-" + LocalDateTime.now().toString().replace(":", "").replace(".", "").substring(0, 20));
            payment.setPaidAt(LocalDateTime.now());
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

            // Kiểm tra tồn kho
            if (cartItem.getQuantity() > sportWear.getStockQuantity()) {
                throw new RuntimeException("Sản phẩm " + sportWear.getName() +
                        " không đủ số lượng trong kho");
            }

            RentalOrderDetail detail = new RentalOrderDetail();
            detail.setRentalOrder(rentalOrder);
            detail.setSportWear(sportWear);
            detail.setQuantity(cartItem.getQuantity());
            detail.setRentalDays(cartItem.getRentalDays());
            detail.setRentalPricePerDay(cartItem.getRentalPricePerDay().doubleValue());
            detail.setSubTotal(cartItem.getRentalPricePerDay().doubleValue() * cartItem.getRentalDays() * cartItem.getQuantity());

            rentalOrderDetailRepository.save(detail);

            // Giảm số lượng tồn kho
            sportWear.setStockQuantity(sportWear.getStockQuantity() - cartItem.getQuantity());
            sportWearRepository.save(sportWear);
        }

        return rentalOrder.getId();
    }

    public void addSportWearToBooking(long bookingId, List<RentalDTO> rentals) {
        Booking booking = bookingRepository.findById(bookingId)
                .orElseThrow(() -> new RuntimeException("Booking không tồn tại"));

        for (RentalDTO rental : rentals) {
            SportWear wear = sportWearService.findById(rental.getSportWearId());
            if (wear == null) continue;

            BookingSportWear bsw = new BookingSportWear();
            bsw.setBooking(booking);
            bsw.setSportWear(wear);
            bsw.setQuantity(rental.getQuantity());
            bsw.setRentalDays(1);
            bsw.setUnitPrice(wear.getRentalPricePerDay());
            bsw.setTotalPrice(wear.getRentalPricePerDay());

            booking.getBookingSportWears().add(bsw);
            booking.setTotalAmount(booking.getTotalAmount().add(bsw.getTotalPrice()));
        }


        bookingRepository.save(booking);
    }

    public void addServiceToBooking(long bookingId, List<RentalDTO> rentals) {
        Booking booking = bookingRepository.findById(bookingId)
                .orElseThrow(() -> new RuntimeException("Booking không tồn tại"));

        for (RentalDTO rental : rentals) {
            utescore.entity.Service serv = serviceService.findById(rental.getServiceId());
            if (serv == null) continue;

            BookingService bs = new BookingService();
            bs.setBooking(booking);
            bs.setService(serv);
            bs.setQuantity(1);
            bs.setUnitPrice(serv.getPrice());
            bs.setTotalPrice(serv.getPrice());

            booking.getBookingServices().add(bs);
        }

        bookingRepository.save(booking);
    }
}
