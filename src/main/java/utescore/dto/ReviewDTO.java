package utescore.dto;

import lombok.Data;
import java.time.LocalDateTime;

@Data
public class ReviewDTO {
    private Long id;
    private Integer rating;
    private String comment;
    private String customerName;
    private String fieldName;
    private Long fieldId;
    private Long bookingId;
    private LocalDateTime createdAt;
}