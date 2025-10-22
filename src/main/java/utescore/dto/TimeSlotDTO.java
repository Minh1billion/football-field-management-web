package utescore.dto;

import lombok.Data;

@Data
public class TimeSlotDTO {
    private String startTime; // "08:00"
    private String endTime;   // "09:00"
    private boolean available;

    public TimeSlotDTO(String startTime, String endTime, boolean available) {
        this.startTime = startTime;
        this.endTime = endTime;
        this.available = available;
    }
}
