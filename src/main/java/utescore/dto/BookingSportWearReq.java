package utescore.dto;

import lombok.Data;

@Data
public class BookingSportWearReq {
	private Long sportWearId;
	private Integer quantity;
	
	// true: sell, false: rent
	private boolean isSell;
}
