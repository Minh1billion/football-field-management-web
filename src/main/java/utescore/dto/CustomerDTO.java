package utescore.dto;

import lombok.Data;

import java.util.List;

@Data
public class CustomerDTO {
    private Long id;
    private String firstName;
    private String lastName;
    private String email; // suy ra từ account nếu cần
    private Integer loyaltyPoints; // có thể lấy từ Loyalty
    private List<Long> bookingIds;
}