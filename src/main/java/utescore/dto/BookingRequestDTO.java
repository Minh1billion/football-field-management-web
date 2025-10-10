package utescore.dto;

import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Data
public class BookingRequestDTO {
    private Long fieldId;
    private Long customerId;
    private LocalDateTime startTime;
    private LocalDateTime endTime;

    // ID dịch vụ kèm theo (số lượng mặc định 1 cho đơn giản)
    private List<Long> serviceIds;
    // ID đồ thể thao thuê (mặc định rentalDays=1, quantity=1)
    private List<Long> sportWearIds;
}