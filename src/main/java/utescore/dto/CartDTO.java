package utescore.dto;

import lombok.Data;
import org.springframework.stereotype.Component;
import org.springframework.web.context.annotation.SessionScope;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Data
@Component
@SessionScope
public class CartDTO {
    private List<RentalDTO> rentalItems = new ArrayList<>();
    private List<SaleDTO> saleItems = new ArrayList<>();
    private BigDecimal totalPrice = BigDecimal.ZERO;

    public Optional<RentalDTO> findRentalItemById(Long sportWearId) {
        return this.rentalItems.stream()
                .filter(item -> item.getSportWearId().equals(sportWearId))
                .findFirst();
    }

    public Optional<SaleDTO> findSaleItemById(Long sportWearId) {
        return this.saleItems.stream()
                .filter(item -> item.getSportWearId().equals(sportWearId))
                .findFirst();
    }

    public List<RentalDTO> getItems() {
        return rentalItems;
    }

    public void setItems(List<RentalDTO> items) {
        this.rentalItems = items;
    }
}