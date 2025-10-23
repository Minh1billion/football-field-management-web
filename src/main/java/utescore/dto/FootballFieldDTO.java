package utescore.dto;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class FootballFieldDTO {
    private Long id;
    private String name;
    private String fieldType;
    private String surfaceType;
    private Integer capacity;
    private BigDecimal pricePerHour;
    private Boolean isActive;
    private Long locationId;
    private String locationName;
    private String locationAddress;
}