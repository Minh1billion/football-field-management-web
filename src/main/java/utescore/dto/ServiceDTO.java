package utescore.dto;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class ServiceDTO {
    private Long id;
    private String name;
    private String serviceType;
    private BigDecimal price;
    private Boolean isAvailable;
}