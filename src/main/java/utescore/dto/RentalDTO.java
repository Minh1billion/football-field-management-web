package utescore.dto;

import lombok.Data;
import java.math.BigDecimal;

@Data
public class RentalDTO {
    private Long serviceId;
    private Long sportWearId;
    private String name;
    private BigDecimal rentalPricePerDay;
    private Integer quantity;
    private Integer rentalDays;
    private BigDecimal totalPrice;
    private String status;
}