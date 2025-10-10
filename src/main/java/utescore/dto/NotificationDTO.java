package utescore.dto;

import lombok.Data;

@Data
public class NotificationDTO {
    private Long id;
    private String message;
    private Long recipientId;
    private String status; // read/unread
}