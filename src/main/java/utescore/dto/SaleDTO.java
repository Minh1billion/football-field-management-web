package utescore.dto;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class SaleDTO {
    private Long sportWearId;
    private String name;
    private int quantity;
    private BigDecimal unitPrice;
    private BigDecimal totalPrice;
    private String status;
    private String imageUrl;

    // Helper method để tính total
    public BigDecimal calculateTotal() {
        if (unitPrice != null && quantity > 0) {
            return unitPrice.multiply(BigDecimal.valueOf(quantity));
        }
        return BigDecimal.ZERO;
    }
}