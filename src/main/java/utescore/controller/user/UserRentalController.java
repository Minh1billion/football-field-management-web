package utescore.controller.user;

import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import utescore.dto.CartDTO;
import utescore.service.RentalService;

@Controller
@RequestMapping("/user/rentals")
@RequiredArgsConstructor
public class UserRentalController {

    private final RentalService rentalService;
    private final CartDTO cartDTO; // Session-scoped

    @GetMapping
    public String rentalSportWearsList(Model model, Pageable pageable) {
        model.addAttribute("sportWears", rentalService.getAvailableSportWears(pageable));
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
}
