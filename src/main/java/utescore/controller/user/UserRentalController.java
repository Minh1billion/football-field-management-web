package utescore.controller.user;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import utescore.dto.CartDTO;
import utescore.dto.RentalDTO;
import utescore.entity.SportWear;
import utescore.service.SportWearService;
import utescore.service.RentalService; // Import your RentalService


@Controller
@RequestMapping("/user/rentals")
@RequiredArgsConstructor
public class UserRentalController {
    private final SportWearService sportWearService;
    private final RentalService rentalService; // Inject RentalService
    private final CartDTO cartDTO; // Inject the Session-Scoped CartDTO

    @GetMapping
    public String rentalSportWearsList(Model model, Pageable pageable) {
        model.addAttribute("sportWears", sportWearService.findAvailableForRent(pageable));
        return "user/rentals/list";
    }

    @GetMapping("/detail/{id}")
    public String rentalSportWearsDetails(Model model,
                                          @PathVariable Long id) {
        SportWear wear = sportWearService.findById(id);
        if (wear == null) {
            // Handle case where item is not found
            return "redirect:/user/rentals";
        }
        model.addAttribute("sportWear", wear);
        return "user/rentals/detail";
    }

    /**
     * Handles adding a product to the rental cart.
     */
    @PostMapping("/add-to-cart/{id}")
    public String addToCart(@PathVariable("id") Long sportWearId,
                            @RequestParam("quantity") int quantity,
                            @RequestParam("rentalDays") int rentalDays,
                            RedirectAttributes redirectAttributes) {

        // 1. Fetch the SportWear to get details (price, stock)
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
            redirectAttributes.addFlashAttribute("errorMessage", "Số lượng yêu cầu vượt quá số lượng tồn kho (" + wear.getStockQuantity() + ").");
            return "redirect:/user/rentals/detail/" + sportWearId;
        }

        // 2. Prepare the RentalDTO item
        RentalDTO newItem = new RentalDTO();
        newItem.setSportWearId(sportWearId);
        newItem.setName(wear.getName());
        newItem.setRentalPricePerDay(wear.getRentalPricePerDay());
        newItem.setQuantity(quantity);
        newItem.setRentalDays(rentalDays);

        // 3. Add item to cart using RentalService (where you put the logic)
        // Note: The logic inside RentalService should be enhanced to check for existing items!
        rentalService.rentSportWear(cartDTO, newItem);

        redirectAttributes.addFlashAttribute("successMessage",
                "Đã thêm " + quantity + " x " + wear.getName() + " vào giỏ hàng thuê.");

        // 4. Redirect to the cart view
        return "redirect:/user/rentals/cart";
    }

    // Add the viewCart method you need for the redirect
    @GetMapping("/cart")
    public String viewCart(Model model) {
        model.addAttribute("cartDTO", cartDTO);
        return "user/rentals/cart"; // Assuming you have a cart.html page
    }
}