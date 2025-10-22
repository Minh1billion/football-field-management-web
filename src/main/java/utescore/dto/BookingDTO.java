package utescore.dto;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
public class BookingDTO {
    private Long id;
    private String bookingCode;
    private Long fieldId;
    private String fieldName;
    private Long customerId;
    private String customerName;
    private LocalDate bookingTime;
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private String notes;
    private BigDecimal totalAmount;
    private String status;
    private String paymentMethod;
}