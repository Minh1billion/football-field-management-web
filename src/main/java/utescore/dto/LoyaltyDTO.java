package utescore.dto;

import lombok.Data;

@Data
public class LoyaltyDTO {
    private Long id;
    private Long customerId;
    private Integer points;
    private String level;
}