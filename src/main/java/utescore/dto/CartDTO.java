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
    private List<RentalDTO> items = new ArrayList<>();
    private BigDecimal totalPrice = BigDecimal.ZERO;

    public Optional<RentalDTO> findItemById(Long sportWearId) {
        return this.items.stream()
                .filter(item -> item.getSportWearId().equals(sportWearId))
                .findFirst();
    }
}