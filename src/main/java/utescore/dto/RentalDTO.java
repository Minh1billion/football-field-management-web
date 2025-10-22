package utescore.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class RentalDTO {
    private Long id;
    private String name;
    private String size;
    private Long price;
}
