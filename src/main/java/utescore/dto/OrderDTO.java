package utescore.dto;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class OrderDTO {
    private Long orderId;
    private Long bookingId;
    private BigDecimal totalPrice;
    private String paymentStatus;
}