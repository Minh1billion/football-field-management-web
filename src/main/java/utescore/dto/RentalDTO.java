package utescore.dto;

import lombok.Data;
import java.math.BigDecimal;

@Data
public class RentalDTO {
    private Long serviceId;
    private Long sportWearId;
    private String name;
    private BigDecimal rentalPricePerDay;
    private int quantity;
    private int rentalDays;
    private BigDecimal totalPrice;
    private String status;
}