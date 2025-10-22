package utescore.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import utescore.dto.CartDTO;
import utescore.dto.RentalDTO;
import utescore.repository.RentalRepository;

import java.math.BigDecimal;
import java.util.Optional;

// Assuming this logic should live in RentalCartService instead of RentalService
// But we'll fix the code structure here based on your prompt.

@Service
@RequiredArgsConstructor
public class RentalService {
    private final RentalRepository rentalRepository;
    // You'll likely need SportWearService here to get the item price/details
    // private final SportWearService sportWearService;

    public long countActiveRentals(String username) {
        return rentalRepository.countActiveRentals(username);
    }

    private BigDecimal calculateItemSubtotal(RentalDTO item) {
        // Subtotal = PricePerDay * Quantity * RentalDays
        BigDecimal days = BigDecimal.valueOf(item.getRentalDays());
        BigDecimal quantity = BigDecimal.valueOf(item.getQuantity());

        return BigDecimal.valueOf(item.getRentalDays())
                .multiply(days)
                .multiply(quantity);
    }

    /**
     * Adds a new rental item to the cart and recalculates the total price.
     * Note: This simple version just adds the item; a robust version would
     * check if the item already exists (by ID) and update the quantity instead.
     */
    public CartDTO rentSportWear(CartDTO cart, RentalDTO newItem) {

        // 1. Check if the item already exists in the cart
        Optional<RentalDTO> existingItemOpt = cart.findItemById(newItem.getSportWearId());

        if (existingItemOpt.isPresent()) {
            RentalDTO existingItem = existingItemOpt.get();
            // 2. If it exists, update the quantity and days
            existingItem.setQuantity(existingItem.getQuantity() + newItem.getQuantity());
            existingItem.setRentalDays(newItem.getRentalDays()); // Often replaces days, or you can take the max/latest.
        } else {
            // 3. If it's new, add it to the list
            cart.getItems().add(newItem);
        }

        // 4. Recalculate the entire cart total using a stream
        BigDecimal newTotal = cart.getItems().stream()
                .map(this::calculateItemSubtotal)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        cart.setTotalPrice(newTotal);

        return cart;
    }

    // We can remove the incomplete private method 'addItemToCart'
}