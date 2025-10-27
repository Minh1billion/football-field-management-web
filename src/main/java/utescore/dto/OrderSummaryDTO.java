package utescore.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class OrderSummaryDTO {
    private Long id;
    private String code;
    private String type; // SALE, RENTAL, BOOKING
    private String customerName;
    private String customerPhone;
    private BigDecimal totalAmount;
    private String orderStatus;
    private String paymentStatus;
    private String paymentMethod;
    private LocalDateTime createdAt;
    private Boolean allReturned; // Cho rental và booking
    private Integer totalItems; // Số lượng sản phẩm
}