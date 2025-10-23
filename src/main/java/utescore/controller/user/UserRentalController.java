package utescore.controller.user;

import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import utescore.dto.CartDTO;
import utescore.dto.RentalDTO;
import utescore.entity.SportWear;
import utescore.service.AccountService;
import utescore.service.BookingService;
import utescore.service.RentalService;
import utescore.service.SportWearService;

import java.util.List;

@Controller
@RequestMapping("/user/rentals")
@RequiredArgsConstructor
public class UserRentalController {

    private final RentalService rentalService;
    private final CartDTO cartDTO;
    private final BookingService bookingService;
    private final SportWearService sportWearService;

    @GetMapping
    public String rentalSportWearsList(Model model, Pageable pageable) {
        model.addAttribute("sportWears", rentalService.getAvailableSportWears(pageable));
        model.addAttribute("myBookings", bookingService.getMyBooking());

        return "user/rentals/list";
    }

    @GetMapping("/detail/{id}")
    public String rentalSportWearDetails(@PathVariable Long id, Model model, RedirectAttributes redirectAttributes) {
        return rentalService.showSportWearDetail(id, model, redirectAttributes);
    }

    @PostMapping("/add-to-cart/{id}")
    public String addToCart(@PathVariable("id") Long sportWearId,
                            @RequestParam("quantity") int quantity,
                            @RequestParam("rentalDays") int rentalDays,
                            RedirectAttributes redirectAttributes) {
        return rentalService.addToCart(cartDTO, sportWearId, quantity, rentalDays, redirectAttributes);
    }

    @GetMapping("/cart")
    public String viewCart(Model model) {
        model.addAttribute("cartDTO", cartDTO);
        return "user/rentals/cart";
    }

    @PostMapping("/update-cart")
    public String updateCart(@RequestParam("sportWearId") Long[] sportWearIds,
                             @RequestParam("quantity") int[] quantities,
                             @RequestParam("rentalDays") int[] rentalDays) {
        for (int i = 0; i < sportWearIds.length; i++) {
            rentalService.updateCartItem(cartDTO, sportWearIds[i], quantities[i], rentalDays[i]);
        }
        return "redirect:/user/rentals/cart";
    }

    @PostMapping("/remove/{id}")
    public String removeFromCart(@PathVariable("id") Long sportWearId) {
        cartDTO.getItems().removeIf(item -> item.getSportWearId().equals(sportWearId));
        rentalService.recalculateCartTotal(cartDTO);
        return "redirect:/user/rentals/cart";
    }

    @PostMapping("/add-to-booking")
    public String addToBooking(@RequestParam("sportWearId") Long sportWearId,
                               @RequestParam(value = "serviceId", required = false) Long serviceId,
                               @RequestParam("quantity") int quantity,
                               @RequestParam("bookingId") long bookingId,
                               RedirectAttributes redirectAttributes) {

        try {
            // Validate quantity
            if (quantity <= 0) {
                redirectAttributes.addFlashAttribute("errorMessage", "Số lượng phải lớn hơn 0");
                return "redirect:/user/rentals";
            }

            if (sportWearId != null) {
                // Kiểm tra stock quantity
                SportWear wear = sportWearService.findById(sportWearId);
                if (wear == null) {
                    redirectAttributes.addFlashAttribute("errorMessage", "Không tìm thấy sản phẩm");
                    return "redirect:/user/rentals";
                }

                if (quantity > wear.getStockQuantity()) {
                    redirectAttributes.addFlashAttribute("errorMessage",
                            "Số lượng yêu cầu (" + quantity + ") vượt quá tồn kho (" + wear.getStockQuantity() + ")");
                    return "redirect:/user/rentals";
                }

                RentalDTO rental = new RentalDTO();
                rental.setSportWearId(sportWearId);
                rental.setQuantity(quantity);
                rental.setRentalDays(1);
                rentalService.addSportWearToBooking(bookingId, List.of(rental));
            }

            if (serviceId != null) {
                RentalDTO rentalServiceDTO = new RentalDTO();
                rentalServiceDTO.setServiceId(serviceId);
                rentalServiceDTO.setQuantity(1);
                rentalService.addServiceToBooking(bookingId, List.of(rentalServiceDTO));
            }

            redirectAttributes.addFlashAttribute("successMessage", "Thêm vào booking thành công!");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", "Thêm vào booking thất bại: " + e.getMessage());
        }

        return "redirect:/user/bookings/" + bookingId;
    }
}
