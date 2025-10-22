package utescore.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class RewardDTO {
    private Long id;
    private Long customerId;
    private Long points;
    private String level;
}
