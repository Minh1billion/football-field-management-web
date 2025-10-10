package utescore.dto;

import lombok.Data;

@Data
public class ReviewDTO {
    private Long id;
    private Integer rating;
    private String comment;
    private String customerName;
    private String fieldName;
}