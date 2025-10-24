package utescore.dto;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class PaymentDTO {
    private Long id;
    private String paymentCode;
    private BigDecimal amount;
    private String paymentMethod;
    private String status;
    private Long bookingId;
    private Long orderId;
    private Long rentalOrderId;
    private Long customerId;
    private String customerName;
    private String notes;
    private LocalDateTime paidAt;
    private LocalDateTime createdAt;
}
