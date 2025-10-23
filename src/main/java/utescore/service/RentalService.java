package utescore.service;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.ui.Model;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import utescore.dto.CartDTO;
import utescore.dto.RentalDTO;
import utescore.entity.Booking;
import utescore.entity.BookingService;
import utescore.entity.BookingSportWear;
import utescore.entity.SportWear;
import utescore.repository.BookingRepository;
import utescore.repository.RentalRepository;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class RentalService {
    private final RentalRepository rentalRepository;
    private final BookingRepository bookingRepository;
    private final SportWearService sportWearService;
    private final ServiceService serviceService;

    public long countActiveRentals(String username) {
        return rentalRepository.countActiveRentals(username);
    }

    public Iterable<SportWear> getAvailableSportWears(Pageable pageable) {
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
        Optional<RentalDTO> itemOpt = cart.findItemById(sportWearId);
        if (itemOpt.isPresent()) {
            RentalDTO item = itemOpt.get();
            item.setQuantity(quantity);
            item.setRentalDays(rentalDays);
            recalculateCartTotal(cart);
        }
    }

    private void updateCart(CartDTO cart, RentalDTO newItem) {
        Optional<RentalDTO> existingItemOpt = cart.findItemById(newItem.getSportWearId());

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
