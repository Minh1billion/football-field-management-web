package utescore.dto;

import java.time.LocalDateTime;

import lombok.Data;

@Data
public class Create_Log {
    private LocalDateTime start;
    private LocalDateTime end;
}
