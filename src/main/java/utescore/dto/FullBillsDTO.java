package utescore.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import lombok.*;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FullBillsDTO {
	private Long paymentId; // lấy từ Payment
	private String paymentType; // lấy từ PaymentMethod CASH, COD ,VNPAY
	private String paymentStatus; // lấy từ PaymentStatus PENDING, COMPLETED, FAILED, REFUNDED, CANCELLED
	private BigDecimal amount; // lấy từ Payment
	
	private String accountName; // lấy từ Account
	private String customerFullName; // lấy từ Customer
	
	private LocalDateTime createdAt; // lấy từ Payment
	
	private String type; // Booking, Order, Rental
	
	
}
