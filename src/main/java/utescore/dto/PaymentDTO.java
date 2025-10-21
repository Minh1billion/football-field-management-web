package utescore.dto;

import lombok.Data;

@Data
public class PaymentDTO {
    private Long id;
    private String paymentMethod;
    private String status;
}