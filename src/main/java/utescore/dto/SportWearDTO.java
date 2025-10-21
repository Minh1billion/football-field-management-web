package utescore.dto;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class SportWearDTO {
    private Long id;
    private String name;
    private String size;
    private BigDecimal rentalPricePerDay;
    private Boolean isAvailableForRent;
}